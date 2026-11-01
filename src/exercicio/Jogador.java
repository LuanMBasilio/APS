package exercicio;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/** O monte de lixo controlado pelo jogador. */
final class Jogador {
    static final int TAMANHO = 124;
    private static final double VELOCIDADE_MAXIMA = 340; // pixels por segundo
    private static final double ACELERACAO = 14; // quanto maior, mais rápido atinge a velocidade alvo

    // A imagem tem muito espaço transparente; a caixa de colisão cobre só o lixo desenhado (em fração do tamanho)
    private static final double CAIXA_X0 = 0.10, CAIXA_X1 = 0.92, CAIXA_Y0 = 0.27, CAIXA_Y1 = 0.73;

    private final BufferedImage imagem;
    private final BufferedImage imagemEspelhada;
    private double x, y, vx, vy;
    private double passo; // fase da animação de "pulinho" ao andar
    private boolean olhandoEsquerda;

    Jogador(BufferedImage original) {
        imagem = Recursos.escalar(original, TAMANHO, TAMANHO);
        imagemEspelhada = Recursos.espelhar(imagem);
    }

    void centralizar(Rectangle area) {
        x = area.getCenterX() - TAMANHO / 2.0;
        y = area.getCenterY() - TAMANHO / 2.0;
        vx = vy = passo = 0;
    }

    /** dirX/dirY valem -1, 0 ou 1 conforme as teclas pressionadas. */
    void atualizar(double dt, double dirX, double dirY, Rectangle area) {
        if (dirX != 0 && dirY != 0) {
            // na diagonal não pode andar mais rápido que em linha reta
            dirX *= Math.sqrt(0.5);
            dirY *= Math.sqrt(0.5);
        }
        double suavizacao = Math.min(1, dt * ACELERACAO);
        vx += (dirX * VELOCIDADE_MAXIMA - vx) * suavizacao;
        vy += (dirY * VELOCIDADE_MAXIMA - vy) * suavizacao;
        x += vx * dt;
        y += vy * dt;

        if (dirX < 0) {
            olhandoEsquerda = true;
        } else if (dirX > 0) {
            olhandoEsquerda = false;
        }
        passo += dt * Math.hypot(vx, vy) / 25;
        limitar(area);
    }

    /** Empurra o jogador para longe (usado quando ele bate na lixeira errada). */
    void empurrar(double dx, double dy, Rectangle area) {
        x += dx;
        y += dy;
        vx = dx * 8;
        vy = dy * 8;
        limitar(area);
    }

    private void limitar(Rectangle area) {
        x = Math.max(area.x - TAMANHO * CAIXA_X0, Math.min(x, area.getMaxX() - TAMANHO * CAIXA_X1));
        y = Math.max(area.y - TAMANHO * CAIXA_Y0, Math.min(y, area.getMaxY() - TAMANHO * CAIXA_Y1));
    }

    Rectangle2D caixa() {
        return new Rectangle2D.Double(x + TAMANHO * CAIXA_X0, y + TAMANHO * CAIXA_Y0,
                TAMANHO * (CAIXA_X1 - CAIXA_X0), TAMANHO * (CAIXA_Y1 - CAIXA_Y0));
    }

    double centroX() {
        return caixa().getCenterX();
    }

    double centroY() {
        return caixa().getCenterY();
    }

    /** Topo do lixo desenhado, já considerando o pulinho; usado para posicionar a etiqueta do item. */
    double topoVisivel() {
        return y + TAMANHO * 0.22 + balanco();
    }

    private double balanco() {
        double intensidade = Math.min(1, Math.hypot(vx, vy) / VELOCIDADE_MAXIMA);
        return -Math.abs(Math.sin(passo)) * 5 * intensidade;
    }

    void desenhar(Graphics2D g, boolean visivel) {
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Double(x + TAMANHO * 0.12, y + TAMANHO * 0.66, TAMANHO * 0.78, TAMANHO * 0.14));
        if (!visivel) {
            return;
        }
        int topo = (int) Math.round(y + balanco());
        BufferedImage atual = olhandoEsquerda ? imagemEspelhada : imagem;
        if (atual != null) {
            g.drawImage(atual, (int) Math.round(x), topo, null);
        } else {
            Rectangle2D c = caixa();
            g.setColor(new Color(120, 90, 60));
            g.fillRoundRect((int) c.getX(), (int) (c.getY() + balanco()), (int) c.getWidth(), (int) c.getHeight(), 20, 20);
        }
    }
}
