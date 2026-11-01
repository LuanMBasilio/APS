package exercicio;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Carregamento e redimensionamento de imagens. */
final class Recursos {
    private Recursos() {}

    /** Procura a imagem em exercicio/recursos (classpath) e, se não achar, na pasta do projeto. Retorna null se não existir. */
    static BufferedImage carregar(String nome) {
        try (InputStream entrada = Recursos.class.getResourceAsStream("recursos/" + nome)) {
            if (entrada != null) {
                return ImageIO.read(entrada);
            }
        } catch (IOException e) {
            // tenta o caminho alternativo abaixo
        }
        for (Path caminho : new Path[] { Path.of("src", "exercicio", "recursos", nome), Path.of(nome) }) {
            try {
                if (Files.exists(caminho)) {
                    return ImageIO.read(caminho.toFile());
                }
            } catch (IOException e) {
                // tenta o próximo
            }
        }
        System.err.println("Imagem não encontrada: " + nome);
        return null;
    }

    /**
     * Redimensiona uma única vez, reduzindo pela metade em etapas: bem mais nítido que deixar o
     * drawImage encolher a imagem original (1000px) a cada quadro, e muito mais rápido.
     */
    static BufferedImage escalar(BufferedImage origem, int largura, int altura) {
        if (origem == null) {
            return null;
        }
        BufferedImage atual = origem;
        int l = origem.getWidth();
        int a = origem.getHeight();
        do {
            l = l > largura ? Math.max(largura, l / 2) : largura;
            a = a > altura ? Math.max(altura, a / 2) : altura;
            BufferedImage proxima = new BufferedImage(l, a, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = proxima.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(atual, 0, 0, l, a, null);
            g.dispose();
            atual = proxima;
        } while (l != largura || a != altura);
        return atual;
    }

    static BufferedImage espelhar(BufferedImage origem) {
        if (origem == null) {
            return null;
        }
        BufferedImage espelhada = new BufferedImage(origem.getWidth(), origem.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = espelhada.createGraphics();
        g.drawImage(origem, origem.getWidth(), 0, -origem.getWidth(), origem.getHeight(), null);
        g.dispose();
        return espelhada;
    }
}
