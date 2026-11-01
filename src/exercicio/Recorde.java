package exercicio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Guarda a maior pontuação num arquivo na pasta do usuário, para sobreviver entre execuções. */
final class Recorde {
    private static final Path ARQUIVO = Path.of(System.getProperty("user.home"), ".reciclagem-recorde");

    private Recorde() {}

    static int carregar() {
        try {
            return Integer.parseInt(Files.readString(ARQUIVO).trim());
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    static void salvar(int pontuacao) {
        try {
            Files.writeString(ARQUIVO, Integer.toString(pontuacao));
        } catch (IOException e) {
            System.err.println("Não foi possível salvar o recorde: " + e.getMessage());
        }
    }
}
