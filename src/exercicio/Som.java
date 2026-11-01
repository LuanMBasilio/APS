package exercicio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

/** Efeitos sonoros sintetizados na hora, sem precisar de arquivos de áudio. */
final class Som {
    private static final float TAXA = 44100f;
    private static final AudioFormat FORMATO = new AudioFormat(TAXA, 16, 1, true, false);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "som");
        t.setDaemon(true);
        return t;
    });

    private static volatile boolean ligado = true;
    private static volatile boolean disponivel = true; // vira false se o computador não tiver saída de áudio

    private Som() {}

    static boolean ligado() {
        return ligado;
    }

    static void alternar() {
        ligado = !ligado;
    }

    /** O tom sobe conforme o combo aumenta, para o jogador "ouvir" a sequência. */
    static void acerto(int combo) {
        double base = 523 * Math.pow(2, Math.min(combo, 12) / 12.0);
        tocar(false, 0.07, 0.25, base, base * 1.5);
    }

    static void erro() {
        tocar(true, 0.13, 0.22, 196, 147);
    }

    static void nivel() {
        tocar(false, 0.08, 0.22, 523, 659, 784, 1047);
    }

    static void contagem() {
        tocar(false, 0.10, 0.20, 440);
    }

    static void largada() {
        tocar(false, 0.25, 0.22, 880);
    }

    static void tique() {
        tocar(false, 0.04, 0.12, 1200);
    }

    static void fim() {
        tocar(false, 0.16, 0.22, 659, 523, 392, 262);
    }

    private static void tocar(boolean aspero, double duracaoNota, double volume, double... frequencias) {
        if (!ligado || !disponivel) {
            return;
        }
        EXECUTOR.execute(() -> {
            byte[] dados = sintetizar(aspero, duracaoNota, volume, frequencias);
            try {
                Clip clip = AudioSystem.getClip();
                clip.open(FORMATO, dados, 0, dados.length);
                clip.addLineListener(evento -> {
                    if (evento.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
                clip.start();
            } catch (Exception | LinkageError e) {
                disponivel = false;
            }
        });
    }

    /** Gera as notas em sequência como PCM 16 bits, com ataque curto e decaimento exponencial. */
    private static byte[] sintetizar(boolean aspero, double duracaoNota, double volume, double[] frequencias) {
        int amostrasPorNota = (int) (duracaoNota * TAXA);
        int ataque = (int) (0.005 * TAXA);
        byte[] dados = new byte[amostrasPorNota * frequencias.length * 2];
        int i = 0;
        for (double frequencia : frequencias) {
            for (int n = 0; n < amostrasPorNota; n++) {
                double t = n / TAXA;
                double onda = Math.sin(2 * Math.PI * frequencia * t);
                if (aspero) {
                    onda = Math.tanh(onda * 4) * 0.7; // quase uma onda quadrada: som de "buzina"
                }
                double envelope = Math.min(1, n / (double) ataque) * Math.exp(-3.0 * n / amostrasPorNota);
                short amostra = (short) (onda * envelope * volume * Short.MAX_VALUE);
                dados[i++] = (byte) amostra;
                dados[i++] = (byte) (amostra >> 8);
            }
        }
        return dados;
    }
}
