package exercicio;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Partículas e textos flutuantes ("+10", "-3s") que dão retorno visual às ações. */
final class Efeitos {
    private static final double GRAVIDADE = 500;

    private static final class Particula {
        double x, y, vx, vy, vida, duracao, tamanho;
        Color cor;
    }

    private static final class Texto {
        double x, y, vida, duracao;
        String conteudo;
        Color cor;
        Font fonte;
    }

    private final List<Particula> particulas = new ArrayList<>();
    private final List<Texto> textos = new ArrayList<>();

    void explosao(double x, double y, Color cor, int quantidade, Random random) {
        for (int i = 0; i < quantidade; i++) {
            Particula p = new Particula();
            double angulo = random.nextDouble() * Math.PI * 2;
            double velocidade = 80 + random.nextDouble() * 240;
            p.x = x;
            p.y = y;
            p.vx = Math.cos(angulo) * velocidade;
            p.vy = Math.sin(angulo) * velocidade - 120;
            p.duracao = 0.5 + random.nextDouble() * 0.4;
            p.tamanho = 4 + random.nextDouble() * 5;
            p.cor = cor;
            particulas.add(p);
        }
    }

    void texto(double x, double y, String conteudo, Color cor, float tamanho) {
        Texto t = new Texto();
        t.x = x;
        t.y = y;
        t.duracao = 1.1;
        t.conteudo = conteudo;
        t.cor = cor;
        t.fonte = new Font(Font.SANS_SERIF, Font.BOLD, Math.round(tamanho));
        textos.add(t);
    }

    void atualizar(double dt) {
        for (Iterator<Particula> it = particulas.iterator(); it.hasNext();) {
            Particula p = it.next();
            p.vida += dt;
            p.vy += GRAVIDADE * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            if (p.vida >= p.duracao) {
                it.remove();
            }
        }
        for (Iterator<Texto> it = textos.iterator(); it.hasNext();) {
            Texto t = it.next();
            t.vida += dt;
            t.y -= 45 * dt;
            if (t.vida >= t.duracao) {
                it.remove();
            }
        }
    }

    void desenhar(Graphics2D g) {
        for (Particula p : particulas) {
            g.setColor(Desenho.comAlfa(p.cor, 1 - p.vida / p.duracao));
            g.fill(new Ellipse2D.Double(p.x - p.tamanho / 2, p.y - p.tamanho / 2, p.tamanho, p.tamanho));
        }
        for (Texto t : textos) {
            // fica opaco na maior parte do tempo e some no final
            double alfa = Math.min(1, (t.duracao - t.vida) * 3);
            g.setFont(t.fonte);
            Desenho.textoComSombra(g, t.conteudo, t.x, t.y, Desenho.comAlfa(t.cor, alfa));
        }
    }

    void limpar() {
        particulas.clear();
        textos.clear();
    }
}
