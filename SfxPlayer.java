package com.seuprojeto.nexus;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/**
 * Efeitos sonoros originais, sintetizados por ondas senoidais em tempo real.
 * Nenhum áudio de terceiros é usado ou reproduzido.
 */
public class SfxPlayer {

    public void playActivate() {
        playTone(new int[]{523, 659, 784}, 90);
    }

    public void playConfirm() {
        playTone(new int[]{880}, 60);
    }

    public void playError() {
        playTone(new int[]{300, 220}, 110);
    }

    private void playTone(int[] frequencies, int durationMsEach) {
        new Thread(() -> {
            int sampleRate = 44100;
            for (int freq : frequencies) {
                int numSamples = (durationMsEach * sampleRate) / 1000;
                short[] samples = new short[numSamples];
                for (int i = 0; i < numSamples; i++) {
                    double angle = 2 * Math.PI * i * freq / sampleRate;
                    double envelope = Math.min(1.0, Math.min((double) i / 200, (double) (numSamples - i) / 200));
                    samples[i] = (short) (Math.sin(angle) * 32767 * 0.3 * envelope);
                }
                AudioTrack track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(samples.length * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();
                track.write(samples, 0, samples.length);
                track.play();
                try {
                    Thread.sleep(durationMsEach);
                } catch (InterruptedException ignored) {
                }
                track.release();
            }
        }).start();
    }
}
