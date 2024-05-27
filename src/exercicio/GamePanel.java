package exercicio;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.*;
import javax.swing.ImageIcon;
import javax.swing.Timer;
import java.util.Random;

class Ponto {
    int x, y, tamanho, valor; //posição eixo X, eixo Y, tamanho e valor do ponto
    Image imagem;
    int identificador; // Identificador do ponto, pode ser passada pra linha de declaração superior se necessário

    public Ponto(int x, int y, int tamanho, int valor, Image imagem, int identificador) {
        this.x = x; //posição do eixo X inicial do ponto
        this.y = y; //posição do eixo Y inicial do ponto
        this.tamanho = tamanho; //tamanho do ponto
        this.valor = valor; //valor do ponto
        this.imagem = imagem; //imagem que fica dentro do ponto
        this.identificador = identificador; // Id que vai ser comparada com o numero aleatorio
    }
    
   //Desenha o que for colocado dentro do parametro, é referenciada na ln 118
    public void desenhar(Graphics g) {
        g.drawImage(imagem, x, y, tamanho, tamanho, null);
        g.setColor(Color.BLACK); // Cor da borda
        g.drawRect(x, y, tamanho, tamanho); // Desenhar a borda
    }

    public boolean colideCom(Ponto outro) { //cria colisão entre objetos, é referenciada em resolver colisão
        return x < outro.x + outro.tamanho && x + tamanho > outro.x &&
               y < outro.y + outro.tamanho && y + tamanho > outro.y;
    }
}

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private int jogadorX = 50;	//posição inicial do jogador no eixo X
    private int jogadorY = 50;	// posição inicial do jogador no eixo Y
    private final int TAMANHO_JOGADOR = 80; // tamanho do jogador (Nome em caixa alta pra indicar que o valor é constante)
    private final int TAMANHO_PONTO = 80; // tamanho do ponto (Nome em caixa alta pra indicar que o valor é constante)
    private Timer temporizador;
    private int pontuacao = 0; //inicializa o score em 0
    private Timer temporizadorContagem;
    private int tempoRestante = 30; // tempo do relogio
    private Image imagemJogador;
    private Random geradorAleatorio;
    private int numeroAleatorio; // Variável para armazenar o número aleatório que vai ser usado nos pontos
    private Image imagemFundo;
    private Ponto[] pontos;

    public GamePanel() {
        this.setPreferredSize(new Dimension(800, 600));
        this.setFocusable(true); //faz com que o painel receba o foco do teclado
        this.addKeyListener(this); //coloca um listener pra "Ouvir" as ações das teclas

        // Inicializar gerador de números aleatórios
        geradorAleatorio = new Random();

        // Gerar um número aleatório entre 0 e 3 no início do jogo
        numeroAleatorio = geradorAleatorio.nextInt(4);// Gera o primeiro numero aleatorio
        System.out.println("Número aleatório inicial gerado: " + numeroAleatorio); //usado pra acompanhar o numero gerado pelo console
        // esse numero que vai selecionar o primeiro material

        // Carregar a imagem do jogador
        ImageIcon iconeJogador = new ImageIcon("player.png");
        imagemJogador = iconeJogador.getImage();

        // Carregar imagens dos pontos
        ImageIcon[] iconesPontos = {
            new ImageIcon("amarelo.png"),
            new ImageIcon("verde.png"),
            new ImageIcon("azul.png"),
            new ImageIcon("vermelho.png")
        };

        // Cria os pontos e os armazena
        pontos = new Ponto[] {
        		
        		//cria os pontos, a posição inicial dos pontos, será sempre a mesma ao iniciar o jogo
            new Ponto(400, 300, TAMANHO_PONTO, 1, iconesPontos[0].getImage(), 1),
            new Ponto(100, 150, TAMANHO_PONTO, 1, iconesPontos[1].getImage(), 2),
            new Ponto(200, 200, TAMANHO_PONTO, 1, iconesPontos[2].getImage(), 3),
            new Ponto(500, 100, TAMANHO_PONTO, 1, iconesPontos[3].getImage(), 0)
        };

        resolverColisoes();
        
        
        
        ImageIcon iconeFundo = new ImageIcon("cidade.jpg"); //caso queira trocar a imagem de fundo, jogar ela na pasta tmedia e colocar o nome dela aqui
        imagemFundo = iconeFundo.getImage();

        temporizador = new Timer(16, this);
        temporizador.start();
        // Cria o contador para o fim do jogo
        temporizadorContagem = new Timer(1000, new ActionListener() { //temporizador
            @Override
            public void actionPerformed(ActionEvent e) {
                tempoRestante--; //diminui o tempo restante em 1
                if (tempoRestante <= 0) {
                    fimDeJogo(); //chama o fim de jogo, evitando que actionPerformed, seja chamada novamente, travando o jogo no final
                }
            }
        });
        temporizadorContagem.start();
    }

    
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        
        g.drawImage(imagemFundo, 0, 0, getWidth(), getHeight(), this); //coloca a imagem de fundo na tela
        
        g.drawImage(imagemJogador, jogadorX, jogadorY, TAMANHO_JOGADOR, TAMANHO_JOGADOR, this); //coloca a imagem do jogador
        g.setColor(Color.BLACK); //evita que a borda do jogador fique com uma cor diferente
        g.drawRect(jogadorX, jogadorY, TAMANHO_JOGADOR, TAMANHO_JOGADOR); // cria a outline ao redor da imagem do  jogador

        // Desenhar os pontos
        for (Ponto ponto : pontos) {
            ponto.desenhar(g);
        }

        // Desenhar a pontuação
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + pontuacao, 10, 20);

        // Desenhar o temporizador
        g.drawString("Timer: " + tempoRestante, 10, 40);

       

        String texto = ""; //Armazena o material na string texto
        switch (numeroAleatorio) {
            case 0:
            	texto = "Plástico"; //Vermelho
            break;
            case 1:
            	texto = "Metal"; //Amarelo
            break;
            case 2:
            	texto = "Vidro"; //Verde
            break;
            case 3:
            	texto = "Papel"; //Azul
            break;
        }
        g.drawString("Material: " + texto, 10, 60); // coloca o material na tela
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        verificarColisao();
        resolverColisoes();
        repaint();
    }

    public void verificarColisao() { //usa a técnica de alinhamento de eixos para verificar se está tendo colisão
        for (Ponto ponto : pontos) {
            if (jogadorX < ponto.x + ponto.tamanho && jogadorX + TAMANHO_JOGADOR > ponto.x &&
                jogadorY < ponto.y + ponto.tamanho && jogadorY + TAMANHO_JOGADOR > ponto.y) { //calcula o tamanho do ponto, do jogador e a posição de ambos para determinar se houve ou não colisão
                // Se houver colisão, mover o ponto para uma nova localização aleatória
                ponto.x = (int) (Math.random() * (getWidth() - ponto.tamanho));
                ponto.y = (int) (Math.random() * (getHeight() - ponto.tamanho));
                


                // Verificar se o identificador do ponto corresponde ao número aleatório
                if (numeroAleatorio == ponto.identificador) {
                    pontuacao += ponto.valor;
                    // Gerar um número aleatório entre 0 e 3
                    numeroAleatorio = geradorAleatorio.nextInt(4);
                    System.out.println("Número aleatório gerado: " + numeroAleatorio); //usado pra acompanhar o numero gerado pelo console
                }

                resolverColisoes();
            }
        }
    }
    // Resolve o problema de colisão de pontos que tava tendo
    public void resolverColisoes() {
        for (int i = 0; i < pontos.length; i++) {
            for (int j = i + 1; j < pontos.length; j++) {
                if (pontos[i].colideCom(pontos[j])) {
                    pontos[j].x = (int) (Math.random() * (getWidth() - pontos[j].tamanho));
                    pontos[j].y = (int) (Math.random() * (getHeight() - pontos[j].tamanho));
                    // Rechecar colisões para este ponto
                    j = i;  // volta o contador pra verificar de novo o ponto que mudou
                }
            }
        }
    }

    private void fimDeJogo() { //finzaliza os timer
        temporizador.stop();
        temporizadorContagem.stop();
        System.out.println("Pontuação final: " + pontuacao); //mostra a pontuação no console, pode ser removida
    }

    // Faz o boneco mexer
    @Override
    public void keyPressed(KeyEvent e) {
        int tecla = e.getKeyCode();
        switch (tecla) {
            case KeyEvent.VK_UP:
                jogadorY = Math.max(jogadorY - 10, 0);
                break;
            case KeyEvent.VK_DOWN:
                jogadorY = Math.min(jogadorY + 10, getHeight() - TAMANHO_JOGADOR);
                break;
            case KeyEvent.VK_LEFT:
                jogadorX = Math.max(jogadorX - 10, 0);
                break;
            case KeyEvent.VK_RIGHT:
                jogadorX = Math.min(jogadorX + 10, getWidth() - TAMANHO_JOGADOR);
                break;
        }
    }

    public void keyReleased(KeyEvent e) {} //tira uns erros que estavão dando pop up no console

    public void keyTyped(KeyEvent e) {} //corrige um erro que estava dando na declaração do gamepanel
}
