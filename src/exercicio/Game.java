package exercicio;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

// Ponto de entrada: dar Run neste arquivo
public class Game {
    public static void main(String[] args) {
        // Componentes Swing devem ser criados na thread de eventos
        SwingUtilities.invokeLater(() -> {
            JFrame janela = new JFrame("Reciclagem");
            GamePanel painel = new GamePanel();

            janela.add(painel);
            janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            janela.setResizable(false);
            janela.setIconImage(Recursos.carregar("verde.png"));
            janela.pack(); // ajusta a janela ao tamanho preferido do painel
            janela.setLocationRelativeTo(null); // centraliza na tela
            janela.setVisible(true);
            painel.requestFocusInWindow();
        });
    }
}
