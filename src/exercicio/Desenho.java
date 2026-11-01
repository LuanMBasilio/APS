package exercicio;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Funções de desenho reaproveitadas pelas telas e entidades. */
final class Desenho {
    private Desenho() {}

    /** Desenha o texto com o centro horizontal em cx e a linha de base em y. */
    static void textoCentralizado(Graphics2D g, String texto, double cx, double y) {
        int largura = g.getFontMetrics().stringWidth(texto);
        g.drawString(texto, (float) (cx - largura / 2.0), (float) y);
    }

    static void textoComSombra(Graphics2D g, String texto, double cx, double y, Color cor) {
        Color sombra = comAlfa(Color.BLACK, cor.getAlpha() / 255.0 * 0.6);
        g.setColor(sombra);
        textoCentralizado(g, texto, cx + 2, y + 2);
        g.setColor(cor);
        textoCentralizado(g, texto, cx, y);
    }

    static void textoADireita(Graphics2D g, String texto, double direita, double y) {
        g.drawString(texto, (float) (direita - g.getFontMetrics().stringWidth(texto)), (float) y);
    }

    /** Etiqueta arredondada centralizada em cx, com o topo em y. */
    static void pilula(Graphics2D g, String texto, double cx, double y, Color fundo, Font fonte) {
        g.setFont(fonte);
        FontMetrics fm = g.getFontMetrics();
        double largura = fm.stringWidth(texto) + 18;
        double altura = fm.getAscent() + fm.getDescent() + 6;
        RoundRectangle2D forma = new RoundRectangle2D.Double(cx - largura / 2, y, largura, altura, altura, altura);
        g.setColor(fundo);
        g.fill(forma);
        g.setColor(new Color(255, 255, 255, 90));
        g.draw(forma);
        g.setColor(contraste(fundo));
        textoCentralizado(g, texto, cx, y + 3 + fm.getAscent());
    }

    /** Texto escuro sobre fundos claros (amarelo) e branco sobre os demais. */
    static Color contraste(Color fundo) {
        double luminancia = 0.299 * fundo.getRed() + 0.587 * fundo.getGreen() + 0.114 * fundo.getBlue();
        return luminancia > 160 ? new Color(30, 30, 30) : Color.WHITE;
    }

    static Color comAlfa(Color cor, double alfa) {
        int a = (int) Math.round(Math.max(0, Math.min(1, alfa)) * 255);
        return new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), a);
    }
}
