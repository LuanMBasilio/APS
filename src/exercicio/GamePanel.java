package exercicio;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Painel principal: controla o laço do jogo, a entrada do teclado e o desenho de cada tela.
 *
 * Regras: um item aparece acima do jogador e ele deve levá-lo até a lixeira da cor certa.
 * Acertos seguidos aumentam o multiplicador e dão tempo extra; erros custam tempo e zeram o combo.
 * A cada nível as lixeiras ficam mais difíceis: primeiro começam a se mover, depois as dicas somem.
 */
public class GamePanel extends JPanel implements ActionListener, KeyListener, FocusListener {
    static final int LARGURA = 960;
    static final int ALTURA = 640;
    static final int ALTURA_HUD = 64;

    private static final double TEMPO_INICIAL = 60;
    private static final double TEMPO_MAXIMO = 99;
    private static final double PENALIDADE_ERRO = 3;
    private static final double DURACAO_CONTAGEM = 3;
    private static final double DURACAO_INVULNERAVEL = 0.6;
    private static final int PONTOS_POR_ACERTO = 10;
    private static final int MULTIPLICADOR_MAXIMO = 5;
    private static final int ACERTOS_POR_NIVEL = 6;
    private static final int NIVEL_LIXEIRAS_MOVEM = 3;
    private static final int NIVEL_SEM_DICAS = 4;

    private static final Font FONTE_PEQUENA = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final Font FONTE_ETIQUETA = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    private static final Font FONTE_MEDIA = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private static final Font FONTE_ITEM = new Font(Font.SANS_SERIF, Font.BOLD, 22);
    private static final Font FONTE_GRANDE = new Font(Font.SANS_SERIF, Font.BOLD, 28);
    private static final Font FONTE_BANNER = new Font(Font.SANS_SERIF, Font.BOLD, 48);
    private static final Font FONTE_TITULO = new Font(Font.SANS_SERIF, Font.BOLD, 72);
    private static final Color COR_SECUNDARIA = new Color(190, 195, 220);
    private static final Color COR_DESTAQUE = new Color(255, 214, 64);
    private static final Color COR_ERRO = new Color(244, 67, 54);
    private static final Color COR_PAINEL = new Color(12, 14, 36, 230);

    private enum Estado { MENU, JOGANDO, PAUSADO, FIM }

    private final Random random = new Random();
    private final Set<Integer> teclas = new HashSet<>();
    private final Efeitos efeitos = new Efeitos();
    private final Rectangle areaJogo = new Rectangle(0, ALTURA_HUD, LARGURA, ALTURA - ALTURA_HUD);
    private final BufferedImage fundo;
    private final Jogador jogador;
    private final List<Lixeira> lixeiras = new ArrayList<>();

    private Estado estado = Estado.MENU;
    private long ultimoQuadro = System.nanoTime();
    private double relogio; // segundos desde que o jogo abriu, usado nas animações

    private int recorde;
    private boolean novoRecorde;

    // Estado da partida atual
    private int pontuacao, acertos, erros, combo, maiorCombo, nivel;
    private double tempoRestante, contagem, invulneravel, tremor, flashErro;
    private Material materialAtual;
    private String itemAtual;
    private String bannerTitulo, bannerSubtitulo;
    private double bannerTempo;

    public GamePanel() {
        setPreferredSize(new Dimension(LARGURA, ALTURA));
        setFocusable(true);
        addKeyListener(this);
        addFocusListener(this);

        fundo = criarFundo();
        jogador = new Jogador(Recursos.carregar("player.png"));
        for (Material material : Material.values()) {
            lixeiras.add(new Lixeira(material, Recursos.carregar(material.imagem)));
        }
        recorde = Recorde.carregar();

        new Timer(15, this).start(); // ~60 quadros por segundo
    }

    /** Pré-desenha o fundo escurecido no tamanho da tela, para não reescalar a imagem a cada quadro. */
    private static BufferedImage criarFundo() {
        BufferedImage imagem = new BufferedImage(LARGURA, ALTURA, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        BufferedImage cidade = Recursos.carregar("cidade.jpg");
        if (cidade != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(cidade, 0, 0, LARGURA, ALTURA, null);
        } else {
            g.setPaint(new GradientPaint(0, 0, new Color(40, 40, 110), 0, ALTURA, new Color(10, 10, 35)));
            g.fillRect(0, 0, LARGURA, ALTURA);
        }
        // escurece para as lixeiras e os textos se destacarem
        g.setColor(new Color(5, 8, 25, 110));
        g.fillRect(0, 0, LARGURA, ALTURA);
        g.dispose();
        return imagem;
    }

    // ------------------------------------------------------------------ partida

    private void iniciarPartida() {
        pontuacao = acertos = erros = combo = maiorCombo = 0;
        nivel = 1;
        tempoRestante = TEMPO_INICIAL;
        contagem = DURACAO_CONTAGEM;
        invulneravel = tremor = flashErro = bannerTempo = 0;
        novoRecorde = false;
        teclas.clear();
        efeitos.limpar();
        jogador.centralizar(areaJogo);
        sortearItem();
        reposicionarLixeiras();
        estado = Estado.JOGANDO;
        Som.contagem();
    }

    private void atualizarPartida(double dt) {
        for (Lixeira lixeira : lixeiras) {
            lixeira.atualizar(dt, areaJogo);
        }
        separarLixeiras();
        bannerTempo = Math.max(0, bannerTempo - dt);

        // Contagem regressiva antes de começar: o jogador ainda não se move
        if (contagem > 0) {
            int antes = (int) Math.ceil(contagem);
            contagem -= dt;
            int depois = (int) Math.ceil(contagem);
            if (depois != antes) {
                if (depois > 0) {
                    Som.contagem();
                } else {
                    Som.largada();
                    mostrarBanner("VAI!", null, 0.8);
                }
            }
            return;
        }

        int segundoAntes = (int) Math.ceil(tempoRestante);
        tempoRestante -= dt;
        if (tempoRestante <= 0) {
            tempoRestante = 0;
            fimDeJogo();
            return;
        }
        if ((int) Math.ceil(tempoRestante) != segundoAntes && segundoAntes <= 5) {
            Som.tique(); // avisa que o tempo está acabando
        }

        double dirX = (pressionada(KeyEvent.VK_RIGHT, KeyEvent.VK_D) ? 1 : 0) - (pressionada(KeyEvent.VK_LEFT, KeyEvent.VK_A) ? 1 : 0);
        double dirY = (pressionada(KeyEvent.VK_DOWN, KeyEvent.VK_S) ? 1 : 0) - (pressionada(KeyEvent.VK_UP, KeyEvent.VK_W) ? 1 : 0);
        jogador.atualizar(dt, dirX, dirY, areaJogo);
        invulneravel = Math.max(0, invulneravel - dt);

        verificarColisoes();
    }

    private void verificarColisoes() {
        Rectangle2D caixaJogador = jogador.caixa();
        for (Lixeira lixeira : lixeiras) {
            if (!lixeira.pronta() || !lixeira.caixa().intersects(caixaJogador)) {
                continue;
            }
            if (lixeira.material == materialAtual) {
                acertar(lixeira);
                return;
            }
            if (invulneravel <= 0) {
                errar(lixeira);
                return;
            }
        }
    }

    private void acertar(Lixeira lixeira) {
        acertos++;
        combo++;
        maiorCombo = Math.max(maiorCombo, combo);
        int ganho = PONTOS_POR_ACERTO * multiplicador();
        pontuacao += ganho;
        double bonus = bonusDeTempo();
        tempoRestante = Math.min(TEMPO_MAXIMO, tempoRestante + bonus);

        efeitos.explosao(lixeira.centroX(), lixeira.centroY(), lixeira.material.cor, 28, random);
        double textoY = alturaTexto(lixeira);
        efeitos.texto(lixeira.centroX(), textoY, "+" + ganho, lixeira.material.cor, 26);
        efeitos.texto(lixeira.centroX(), textoY + 24, String.format("+%.1fs", bonus), Color.WHITE, 15);
        Som.acerto(combo);

        int nivelNovo = 1 + acertos / ACERTOS_POR_NIVEL;
        if (nivelNovo > nivel) {
            nivel = nivelNovo;
            anunciarNivel();
        }
        sortearItem();
        reposicionarLixeiras();
    }

    private void errar(Lixeira lixeira) {
        erros++;
        combo = 0;
        tempoRestante = Math.max(0, tempoRestante - PENALIDADE_ERRO);
        invulneravel = DURACAO_INVULNERAVEL;
        tremor = 10;
        flashErro = 1;

        // empurra o jogador para longe da lixeira errada
        double dx = jogador.centroX() - lixeira.centroX();
        double dy = jogador.centroY() - lixeira.centroY();
        double distancia = Math.max(1, Math.hypot(dx, dy));
        jogador.empurrar(dx / distancia * 40, dy / distancia * 40, areaJogo);

        // ensina onde o item deveria ir
        efeitos.texto(jogador.centroX(), Math.max(ALTURA_HUD + 56, jogador.topoVisivel() - 36),
                itemAtual + " vai no " + materialAtual.nome.toLowerCase() + "!", materialAtual.cor, 17);
        efeitos.texto(lixeira.centroX(), alturaTexto(lixeira), "-" + (int) PENALIDADE_ERRO + "s", COR_ERRO, 24);
        Som.erro();
    }

    /** Texto logo acima da lixeira, mas sem ficar escondido atrás do placar. */
    private static double alturaTexto(Lixeira lixeira) {
        return Math.max(ALTURA_HUD + 30, lixeira.y - 6);
    }

    private void fimDeJogo() {
        estado = Estado.FIM;
        teclas.clear();
        if (pontuacao > recorde) {
            recorde = pontuacao;
            novoRecorde = true;
            Recorde.salvar(recorde);
        }
        Som.fim();
    }

    private void sortearItem() {
        String anterior = itemAtual;
        do {
            materialAtual = Material.values()[random.nextInt(Material.values().length)];
            itemAtual = materialAtual.itemAleatorio(random);
        } while (itemAtual.equals(anterior)); // evita repetir o mesmo item duas vezes seguidas
    }

    /** Espalha as lixeiras sem sobrepor umas às outras nem nascer em cima do jogador. */
    private void reposicionarLixeiras() {
        double velocidade = velocidadeLixeiras();
        Rectangle2D caixaJogador = jogador.caixa();
        Rectangle2D zonaJogador = new Rectangle2D.Double(caixaJogador.getX() - 110, caixaJogador.getY() - 110,
                caixaJogador.getWidth() + 220, caixaJogador.getHeight() + 220);
        List<Rectangle2D> ocupadas = new ArrayList<>();

        double minX = areaJogo.x + 16;
        double maxX = areaJogo.getMaxX() - Lixeira.LARGURA - 16;
        double minY = areaJogo.y + 16;
        double maxY = areaJogo.getMaxY() - Lixeira.ALTURA - Lixeira.ESPACO_ROTULO;
        for (Lixeira lixeira : lixeiras) {
            double x = 0, y = 0;
            for (int tentativa = 0; tentativa < 300; tentativa++) {
                x = minX + random.nextDouble() * (maxX - minX);
                y = minY + random.nextDouble() * (maxY - minY);
                Rectangle2D espaco = new Rectangle2D.Double(x - 30, y - 30, Lixeira.LARGURA + 60, Lixeira.ALTURA + 60);
                if (!espaco.intersects(zonaJogador) && ocupadas.stream().noneMatch(espaco::intersects)) {
                    break;
                }
            }
            if (lixeira.pronta()) {
                efeitos.explosao(lixeira.centroX(), lixeira.centroY(), new Color(255, 255, 255, 160), 6, random);
            }
            lixeira.posicionar(x, y, velocidade, random);
            ocupadas.add(new Rectangle2D.Double(x, y, Lixeira.LARGURA, Lixeira.ALTURA));
        }
    }

    /** Quando duas lixeiras em movimento se encostam, cada uma segue para o lado oposto. */
    private void separarLixeiras() {
        for (int i = 0; i < lixeiras.size(); i++) {
            for (int j = i + 1; j < lixeiras.size(); j++) {
                Lixeira a = lixeiras.get(i);
                Lixeira b = lixeiras.get(j);
                if (!a.limites().intersects(b.limites())) {
                    continue;
                }
                double dx = b.centroX() - a.centroX();
                double dy = b.centroY() - a.centroY();
                double distancia = Math.hypot(dx, dy);
                if (distancia == 0) {
                    dx = 1;
                    distancia = 1;
                }
                dx /= distancia;
                dy /= distancia;
                double va = Math.hypot(a.vx, a.vy);
                double vb = Math.hypot(b.vx, b.vy);
                a.vx = -dx * va;
                a.vy = -dy * va;
                b.vx = dx * vb;
                b.vy = dy * vb;
            }
        }
    }

    private void anunciarNivel() {
        String descricao;
        if (nivel == NIVEL_LIXEIRAS_MOVEM) {
            descricao = "As lixeiras começaram a se mover!";
        } else if (nivel == NIVEL_SEM_DICAS) {
            descricao = "Sem dicas agora: lembre das cores!";
        } else if (nivel > NIVEL_SEM_DICAS) {
            descricao = "Lixeiras mais rápidas!";
        } else {
            descricao = "Continue acertando para manter o combo!";
        }
        mostrarBanner("NÍVEL " + nivel, descricao, 2.2);
        Som.nivel();
    }

    private void mostrarBanner(String titulo, String subtitulo, double duracao) {
        bannerTitulo = titulo;
        bannerSubtitulo = subtitulo;
        bannerTempo = duracao;
    }

    /** x1 até x5: sobe um a cada 3 acertos seguidos. */
    private int multiplicador() {
        return Math.min(MULTIPLICADOR_MAXIMO, 1 + Math.max(0, combo - 1) / 3);
    }

    /** O tempo extra por acerto diminui a cada nível, para a partida não durar para sempre. */
    private double bonusDeTempo() {
        return Math.max(0.5, 2.0 - 0.25 * (nivel - 1));
    }

    private double velocidadeLixeiras() {
        if (nivel < NIVEL_LIXEIRAS_MOVEM) {
            return 0;
        }
        return Math.min(170, 45 + (nivel - NIVEL_LIXEIRAS_MOVEM) * 25);
    }

    private boolean mostrarDicas() {
        return nivel < NIVEL_SEM_DICAS;
    }

    // ------------------------------------------------------------------ laço

    @Override
    public void actionPerformed(ActionEvent e) {
        long agora = System.nanoTime();
        // limita o passo para o jogo não "pular" se a janela travar por um instante
        double dt = Math.min(0.05, (agora - ultimoQuadro) / 1e9);
        ultimoQuadro = agora;
        relogio += dt;

        if (estado == Estado.JOGANDO) {
            atualizarPartida(dt);
        }
        if (estado != Estado.PAUSADO) {
            efeitos.atualizar(dt);
            tremor = Math.max(0, tremor - dt * 40);
            flashErro = Math.max(0, flashErro - dt * 3);
        }
        repaint();
        Toolkit.getDefaultToolkit().sync(); // evita engasgos de animação em alguns sistemas
    }

    // ------------------------------------------------------------------ desenho

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g.drawImage(fundo, 0, 0, null);
        if (estado == Estado.MENU) {
            desenharMenu(g);
        } else {
            desenharMundo(g);
            desenharHud(g);
            if (estado == Estado.PAUSADO) {
                desenharPausa(g);
            } else if (estado == Estado.FIM) {
                desenharFim(g);
            }
        }
        desenharIndicadorSom(g);
        g.dispose();
    }

    private void desenharMundo(Graphics2D g) {
        Graphics2D mundo = (Graphics2D) g.create();
        if (tremor > 0) {
            mundo.translate((random.nextDouble() - 0.5) * tremor, (random.nextDouble() - 0.5) * tremor);
        }

        // desenha de cima para baixo, para quem está mais "à frente" ficar por cima
        double baseJogador = jogador.caixa().getMaxY();
        boolean rotulos = mostrarDicas();
        for (Lixeira lixeira : lixeiras) {
            if (lixeira.y + Lixeira.ALTURA <= baseJogador) {
                lixeira.desenhar(mundo, rotulos);
            }
        }
        // pisca enquanto está invulnerável depois de um erro
        boolean visivel = invulneravel <= 0 || (int) (relogio * 16) % 2 == 0;
        jogador.desenhar(mundo, visivel);
        for (Lixeira lixeira : lixeiras) {
            if (lixeira.y + Lixeira.ALTURA > baseJogador) {
                lixeira.desenhar(mundo, rotulos);
            }
        }

        if (estado != Estado.FIM) {
            Color fundoEtiqueta = mostrarDicas() ? materialAtual.cor : new Color(30, 34, 64);
            Desenho.pilula(mundo, itemAtual, jogador.centroX(), jogador.topoVisivel() - 26, fundoEtiqueta, FONTE_ETIQUETA);
        }
        efeitos.desenhar(mundo);
        mundo.dispose();

        if (flashErro > 0) {
            g.setColor(Desenho.comAlfa(COR_ERRO, flashErro * 0.35));
            g.fillRect(0, 0, LARGURA, ALTURA);
        }
        if (contagem > 0 && estado == Estado.JOGANDO) {
            desenharContagem(g);
        }
        if (bannerTempo > 0) {
            desenharBanner(g);
        }
    }

    private void desenharContagem(Graphics2D g) {
        int numero = (int) Math.ceil(contagem);
        double fracao = contagem - (numero - 1); // vai de 1 a 0 dentro de cada segundo
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (80 + 60 * fracao)));
        Desenho.textoComSombra(g, String.valueOf(numero), LARGURA / 2.0, ALTURA / 2.0 + 40,
                Desenho.comAlfa(Color.WHITE, Math.min(1, fracao * 1.5)));
        g.setFont(FONTE_MEDIA);
        Desenho.textoComSombra(g, "Leve o item até a lixeira certa", LARGURA / 2.0, ALTURA / 2.0 + 100, COR_SECUNDARIA);
    }

    private void desenharBanner(Graphics2D g) {
        double alfa = Math.min(1, bannerTempo * 2.5);
        g.setFont(FONTE_BANNER);
        Desenho.textoComSombra(g, bannerTitulo, LARGURA / 2.0, ALTURA * 0.38, Desenho.comAlfa(COR_DESTAQUE, alfa));
        if (bannerSubtitulo != null) {
            g.setFont(FONTE_MEDIA);
            Desenho.textoComSombra(g, bannerSubtitulo, LARGURA / 2.0, ALTURA * 0.38 + 36, Desenho.comAlfa(Color.WHITE, alfa));
        }
    }

    private void desenharHud(Graphics2D g) {
        g.setColor(new Color(8, 10, 28, 215));
        g.fillRect(0, 0, LARGURA, ALTURA_HUD);
        g.setColor(new Color(255, 255, 255, 40));
        g.fillRect(0, ALTURA_HUD - 1, LARGURA, 1);

        // pontos
        rotulo(g, "PONTOS", 20, 22);
        g.setFont(FONTE_GRANDE);
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(pontuacao), 20, 52);

        // combo
        int multiplicador = multiplicador();
        rotulo(g, "COMBO", 150, 22);
        g.setFont(FONTE_GRANDE);
        g.setColor(multiplicador > 1 ? COR_DESTAQUE : Color.WHITE);
        String textoMultiplicador = "x" + multiplicador;
        g.drawString(textoMultiplicador, 150, 52);
        if (combo > 1) {
            int deslocamento = g.getFontMetrics().stringWidth(textoMultiplicador) + 8;
            g.setFont(FONTE_PEQUENA);
            g.setColor(COR_SECUNDARIA);
            g.drawString(combo + " seguidos", 150 + deslocamento, 52);
        }

        // item atual, com o material como dica nos primeiros níveis
        g.setFont(FONTE_PEQUENA);
        g.setColor(COR_SECUNDARIA);
        Desenho.textoCentralizado(g, "LEVE PARA A LIXEIRA CERTA", LARGURA / 2.0, 22);
        g.setFont(FONTE_ITEM);
        String dica = mostrarDicas() ? "  (" + materialAtual.nome + ")" : "";
        int larguraItem = g.getFontMetrics().stringWidth(itemAtual);
        int inicio = (LARGURA - larguraItem - g.getFontMetrics().stringWidth(dica)) / 2;
        g.setColor(Color.WHITE);
        g.drawString(itemAtual, inicio, 50);
        g.setColor(materialAtual.cor);
        g.drawString(dica, inicio + larguraItem, 50);

        // tempo
        int barraX = LARGURA - 250;
        int barraLargura = 230;
        rotulo(g, "TEMPO", barraX, 22);
        g.setFont(FONTE_PEQUENA);
        g.setColor(Color.WHITE);
        Desenho.textoADireita(g, (int) Math.ceil(tempoRestante) + "s", barraX + barraLargura, 22);

        double fracao = Math.min(1, tempoRestante / TEMPO_INICIAL);
        Color corBarra = fracao > 0.5 ? new Color(76, 175, 80) : fracao > 0.2 ? new Color(255, 193, 7) : COR_ERRO;
        if (tempoRestante < 10 && Math.sin(relogio * 12) > 0) {
            corBarra = corBarra.brighter();
        }
        g.setColor(new Color(255, 255, 255, 40));
        g.fill(new RoundRectangle2D.Double(barraX, 29, barraLargura, 12, 12, 12));
        g.setColor(corBarra);
        g.fill(new RoundRectangle2D.Double(barraX, 29, barraLargura * fracao, 12, 12, 12));

        g.setFont(FONTE_PEQUENA);
        g.setColor(COR_SECUNDARIA);
        g.drawString("NÍVEL " + nivel, barraX, 58);
        Desenho.textoADireita(g, "RECORDE " + Math.max(recorde, pontuacao), barraX + barraLargura, 58);
    }

    private static void rotulo(Graphics2D g, String texto, int x, int y) {
        g.setFont(FONTE_PEQUENA);
        g.setColor(COR_SECUNDARIA);
        g.drawString(texto, x, y);
    }

    private void desenharMenu(Graphics2D g) {
        escurecer(g, 0.35);
        double cx = LARGURA / 2.0;

        g.setFont(FONTE_TITULO);
        g.setColor(new Color(46, 125, 50));
        Desenho.textoCentralizado(g, "RECICLAGEM", cx + 4, 154);
        g.setColor(Color.WHITE);
        Desenho.textoCentralizado(g, "RECICLAGEM", cx, 150);
        g.setFont(FONTE_MEDIA);
        Desenho.textoComSombra(g, "Leve cada resíduo até a lixeira da cor certa!", cx, 192, COR_SECUNDARIA);

        // legenda com as quatro lixeiras, flutuando de leve
        int espaco = 150;
        double inicio = cx - espaco * (lixeiras.size() - 1) / 2.0;
        for (int i = 0; i < lixeiras.size(); i++) {
            Lixeira lixeira = lixeiras.get(i);
            double x = inicio + i * espaco;
            double y = 230 + Math.sin(relogio * 2 + i) * 5;
            lixeira.desenharEm(g, x - Lixeira.LARGURA / 2.0, y, 1);
            Desenho.pilula(g, lixeira.material.nome.toUpperCase(), x, 340, lixeira.material.cor, FONTE_PEQUENA);
        }

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        g.setColor(COR_SECUNDARIA);
        Desenho.textoCentralizado(g, "Acertos seguidos aumentam o multiplicador e dão tempo extra.", cx, 400);
        Desenho.textoCentralizado(g, "Errar a lixeira custa " + (int) PENALIDADE_ERRO + " segundos e zera o combo.", cx, 424);
        Desenho.textoCentralizado(g, "A cada nível fica mais difícil: as lixeiras se movem e as dicas somem.", cx, 448);

        g.setFont(FONTE_PEQUENA);
        Desenho.textoCentralizado(g, "SETAS ou WASD: mover     P: pausar     M: som     ESC: sair", cx, 490);

        double pulso = 0.55 + 0.45 * Math.sin(relogio * 4);
        g.setFont(FONTE_GRANDE);
        Desenho.textoComSombra(g, "Pressione ENTER para jogar", cx, 550, Desenho.comAlfa(COR_DESTAQUE, pulso));
        if (recorde > 0) {
            g.setFont(FONTE_MEDIA);
            Desenho.textoComSombra(g, "Recorde: " + recorde, cx, 594, Color.WHITE);
        }
    }

    private void desenharPausa(Graphics2D g) {
        escurecer(g, 0.6);
        g.setFont(FONTE_BANNER);
        Desenho.textoComSombra(g, "PAUSADO", LARGURA / 2.0, ALTURA / 2.0 - 10, Color.WHITE);
        g.setFont(FONTE_MEDIA);
        Desenho.textoComSombra(g, "P ou ESC: continuar     Q: voltar ao menu", LARGURA / 2.0, ALTURA / 2.0 + 34, COR_SECUNDARIA);
    }

    private void desenharFim(Graphics2D g) {
        escurecer(g, 0.55);
        double cx = LARGURA / 2.0;
        int largura = 460;
        int altura = 400;
        int topo = (ALTURA - altura) / 2 + 10;
        RoundRectangle2D painel = new RoundRectangle2D.Double(cx - largura / 2.0, topo, largura, altura, 28, 28);
        g.setColor(COR_PAINEL);
        g.fill(painel);
        g.setColor(new Color(255, 255, 255, 50));
        g.setStroke(new BasicStroke(2));
        g.draw(painel);

        g.setFont(FONTE_GRANDE);
        Desenho.textoComSombra(g, "FIM DE JOGO", cx, topo + 50, Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 64));
        Desenho.textoComSombra(g, String.valueOf(pontuacao), cx, topo + 130, COR_DESTAQUE);
        g.setFont(FONTE_MEDIA);
        if (novoRecorde) {
            double pulso = 0.6 + 0.4 * Math.sin(relogio * 6);
            Desenho.textoComSombra(g, "NOVO RECORDE!", cx, topo + 164, Desenho.comAlfa(COR_DESTAQUE, pulso));
        } else {
            Desenho.textoComSombra(g, "Recorde: " + recorde, cx, topo + 164, COR_SECUNDARIA);
        }

        int tentativas = acertos + erros;
        int precisao = tentativas == 0 ? 0 : Math.round(100f * acertos / tentativas);
        String[][] estatisticas = {
            { "Acertos", String.valueOf(acertos) },
            { "Erros", String.valueOf(erros) },
            { "Precisão", precisao + "%" },
            { "Maior combo", String.valueOf(maiorCombo) },
            { "Nível alcançado", String.valueOf(nivel) },
        };
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        int y = topo + 206;
        for (String[] linha : estatisticas) {
            g.setColor(COR_SECUNDARIA);
            g.drawString(linha[0], (int) cx - 150, y);
            g.setColor(Color.WHITE);
            Desenho.textoADireita(g, linha[1], cx + 150, y);
            y += 26;
        }

        g.setFont(FONTE_PEQUENA);
        g.setColor(COR_SECUNDARIA);
        Desenho.textoCentralizado(g, "ENTER: jogar de novo     ESC: menu", cx, topo + altura - 24);
    }

    private void desenharIndicadorSom(Graphics2D g) {
        g.setFont(FONTE_PEQUENA);
        g.setColor(new Color(255, 255, 255, 110));
        Desenho.textoADireita(g, Som.ligado() ? "Som: ligado (M)" : "Som: desligado (M)", LARGURA - 12, ALTURA - 10);
    }

    private static void escurecer(Graphics2D g, double opacidade) {
        Graphics2D camada = (Graphics2D) g.create();
        camada.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacidade));
        camada.setColor(new Color(5, 6, 20));
        camada.fillRect(0, 0, LARGURA, ALTURA);
        camada.dispose();
    }

    // ------------------------------------------------------------------ teclado

    private boolean pressionada(int... codigos) {
        for (int codigo : codigos) {
            if (teclas.contains(codigo)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int tecla = e.getKeyCode();
        teclas.add(tecla);
        if (tecla == KeyEvent.VK_M) {
            Som.alternar();
            return;
        }
        switch (estado) {
            case MENU:
                if (tecla == KeyEvent.VK_ENTER || tecla == KeyEvent.VK_SPACE) {
                    iniciarPartida();
                } else if (tecla == KeyEvent.VK_ESCAPE) {
                    Window janela = SwingUtilities.getWindowAncestor(this);
                    janela.dispatchEvent(new WindowEvent(janela, WindowEvent.WINDOW_CLOSING));
                }
                break;
            case JOGANDO:
                if (tecla == KeyEvent.VK_P || tecla == KeyEvent.VK_ESCAPE) {
                    estado = Estado.PAUSADO;
                }
                break;
            case PAUSADO:
                if (tecla == KeyEvent.VK_P || tecla == KeyEvent.VK_ESCAPE || tecla == KeyEvent.VK_ENTER) {
                    estado = Estado.JOGANDO;
                } else if (tecla == KeyEvent.VK_Q) {
                    estado = Estado.MENU;
                }
                break;
            case FIM:
                if (tecla == KeyEvent.VK_ENTER || tecla == KeyEvent.VK_SPACE) {
                    iniciarPartida();
                } else if (tecla == KeyEvent.VK_ESCAPE) {
                    estado = Estado.MENU;
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        teclas.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void focusGained(FocusEvent e) {}

    /** Se a janela perder o foco, solta as teclas e pausa, senão o jogador "anda sozinho". */
    @Override
    public void focusLost(FocusEvent e) {
        teclas.clear();
        if (estado == Estado.JOGANDO) {
            estado = Estado.PAUSADO;
        }
    }
}
