package exercicio;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Lixeira de coleta seletiva. A partir de certo nível ela passa a se mover e quicar nas bordas. */
final class Lixeira {
    // Mantém a proporção das imagens originais (~860x1170), que antes eram achatadas num quadrado
    static final int LARGURA = 74;
    static final int ALTURA = 100;
    static final int ESPACO_ROTULO = 26; // espaço abaixo da lixeira para o nome do material

    private static final Font FONTE_ROTULO = new Font(Font.SANS_SERIF, Font.BOLD, 11);

    final Material material;
    final BufferedImage imagem;
    double x, y, vx, vy;
    private double surgimento = 1; // 0 = acabou de aparecer, 1 = animação concluída

    Lixeira(Material material, BufferedImage original) {
        this.material = material;
        this.imagem = Recursos.escalar(original, LARGURA, ALTURA);
    }

    void posicionar(double x, double y, double velocidade, Random random) {
        this.x = x;
        this.y = y;
        double angulo = random.nextDouble() * Math.PI * 2;
        vx = Math.cos(angulo) * velocidade;
        vy = Math.sin(angulo) * velocidade;
        surgimento = 0;
    }

    /** Só aceita colisão depois que terminou de aparecer, para não "pegar" o jogador de surpresa. */
    boolean pronta() {
        return surgimento >= 1;
    }

    void atualizar(double dt, Rectangle area) {
        surgimento = Math.min(1, surgimento + dt * 5);
        x += vx * dt;
        y += vy * dt;

        double minX = area.x + 8;
        double maxX = area.getMaxX() - LARGURA - 8;
        double minY = area.y + 8;
        double maxY = area.getMaxY() - ALTURA - ESPACO_ROTULO;
        if (x < minX) {
            x = minX;
            vx = Math.abs(vx);
        } else if (x > maxX) {
            x = maxX;
            vx = -Math.abs(vx);
        }
        if (y < minY) {
            y = minY;
            vy = Math.abs(vy);
        } else if (y > maxY) {
            y = maxY;
            vy = -Math.abs(vy);
        }
    }

    Rectangle2D limites() {
        return new Rectangle2D.Double(x, y, LARGURA, ALTURA);
    }

    /** Caixa de colisão: só o corpo da lixeira, sem a tampa aberta nem a sombra da imagem. */
    Rectangle2D caixa() {
        return new Rectangle2D.Double(x + LARGURA * 0.12, y + ALTURA * 0.30, LARGURA * 0.74, ALTURA * 0.58);
    }

    double centroX() {
        return x + LARGURA / 2.0;
    }

    double centroY() {
        return y + ALTURA / 2.0;
    }

    void desenhar(Graphics2D g, boolean comRotulo) {
        desenharEm(g, x, y, surgir(surgimento));
        if (comRotulo) {
            Desenho.pilula(g, material.nome.toUpperCase(), centroX(), y + ALTURA + 3, material.cor, FONTE_ROTULO);
        }
    }

    /** Desenha a imagem com escala a partir da base, para a lixeira "brotar" do chão. */
    void desenharEm(Graphics2D g, double px, double py, double escala) {
        AffineTransform original = g.getTransform();
        g.translate(px + LARGURA / 2.0, py + ALTURA);
        g.scale(escala, escala);
        if (imagem != null) {
            g.drawImage(imagem, -LARGURA / 2, -ALTURA, null);
        } else {
            g.setColor(material.cor);
            g.fillRoundRect(-LARGURA / 2, -ALTURA, LARGURA, ALTURA, 14, 14);
        }
        g.setTransform(original);
    }

    /** Curva "ease out back": passa um pouco do tamanho final e volta, dando um efeito elástico. */
    private static double surgir(double t) {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }
}
