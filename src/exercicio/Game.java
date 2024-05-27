package exercicio;

import javax.swing.JFrame;
//dar Run neste arquivo
public class Game{
    public static void main(String[] args) {
        JFrame frame = new JFrame("APS: Reciclaagem"); //cria um frame e coloca o titulo
        GamePanel gamePanel = new GamePanel(); //cria o objeto gamePanel da classe GamePanel
        
        frame.add(gamePanel); //coloca GamePanel no frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600); //define o tamanho
        frame.setVisible(true); //define como visivel
    }
}
