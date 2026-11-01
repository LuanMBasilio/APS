package exercicio;

import java.awt.Color;
import java.util.List;
import java.util.Random;

/** Materiais recicláveis, com as cores padrão da coleta seletiva (Resolução CONAMA 275). */
enum Material {
    PLASTICO("Plástico", new Color(0xE53935), "vermelho.png",
            "Garrafa PET", "Sacola plástica", "Pote de margarina", "Embalagem de xampu", "Tampa plástica", "Balde quebrado"),
    METAL("Metal", new Color(0xFDD835), "amarelo.png",
            "Lata de refrigerante", "Lata de sardinha", "Tampa de metal", "Papel alumínio", "Lata de milho", "Arame"),
    VIDRO("Vidro", new Color(0x43A047), "verde.png",
            "Garrafa de vidro", "Pote de geleia", "Frasco de perfume", "Pote de palmito", "Garrafa de azeite"),
    PAPEL("Papel", new Color(0x1E88E5), "azul.png",
            "Jornal", "Caixa de papelão", "Revista", "Folha de caderno", "Envelope", "Caixa de cereal");

    final String nome;
    final Color cor;
    final String imagem;
    private final List<String> itens;

    Material(String nome, Color cor, String imagem, String... itens) {
        this.nome = nome;
        this.cor = cor;
        this.imagem = imagem;
        this.itens = List.of(itens);
    }

    String itemAleatorio(Random random) {
        return itens.get(random.nextInt(itens.size()));
    }
}
