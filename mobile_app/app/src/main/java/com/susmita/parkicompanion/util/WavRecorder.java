package com.susmita.parkicompanion.util;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public final class WavRecorder {

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNELS = 1;
    private static final int BITS_PER_SAMPLE = 16;

    private final File outputFile;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private volatile boolean recording;
    private volatile IOException recordingError;

    public WavRecorder(File outputFile) {
        this.outputFile = outputFile;
    }

    public synchronized void start() throws IOException {
        if (recording) {
            throw new IllegalStateException("Recording is already in progress");
        }
        int minimumBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (minimumBuffer <= 0) {
            throw new IOException("Audio recording is not supported");
        }

        int bufferSize = Math.max(minimumBuffer, SAMPLE_RATE / 2);
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release();
            audioRecord = null;
            throw new IOException("Could not initialize audio recording");
        }

        try {
            writeHeader(outputFile, 0);
        } catch (IOException error) {
            audioRecord.release();
            audioRecord = null;
            throw error;
        }

        recordingError = null;
        recording = true;
        audioRecord.startRecording();
        recordingThread = new Thread(() -> recordAudio(bufferSize), "voice-wav-recorder");
        recordingThread.start();
    }

    public synchronized File stop() throws IOException {
        if (!recording) {
            return outputFile;
        }
        recording = false;
        if (audioRecord != null) {
            audioRecord.stop();
        }
        Thread thread = recordingThread;
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IOException("Recording could not stop", error);
            }
        }
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        recordingThread = null;
        if (recordingError != null) {
            throw recordingError;
        }
        patchSizes(outputFile);
        return outputFile;
    }

    private void recordAudio(int bufferSize) {
        byte[] buffer = new byte[bufferSize];
        try (RandomAccessFile output = new RandomAccessFile(outputFile, "rw")) {
            output.seek(44);
            while (recording) {
                int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    output.write(buffer, 0, bytesRead);
                } else if (bytesRead < 0) {
                    recordingError = new IOException("Audio recording failed: " + bytesRead);
                    recording = false;
                }
            }
        } catch (IOException error) {
            recordingError = error;
            recording = false;
        }
    }

    private static void writeHeader(File file, long dataLength) throws IOException {
        try (RandomAccessFile output = new RandomAccessFile(file, "rw")) {
            output.setLength(0);
            output.writeBytes("RIFF");
            writeLittleEndianInt(output, (int) (36 + dataLength));
            output.writeBytes("WAVE");
            output.writeBytes("fmt ");
            writeLittleEndianInt(output, 16);
            writeLittleEndianShort(output, (short) 1);
            writeLittleEndianShort(output, (short) CHANNELS);
            writeLittleEndianInt(output, SAMPLE_RATE);
            writeLittleEndianInt(output, SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8);
            writeLittleEndianShort(output, (short) (CHANNELS * BITS_PER_SAMPLE / 8));
            writeLittleEndianShort(output, (short) BITS_PER_SAMPLE);
            output.writeBytes("data");
            writeLittleEndianInt(output, (int) dataLength);
        }
    }

    private static void patchSizes(File file) throws IOException {
        long dataLength = Math.max(0, file.length() - 44);
        try (RandomAccessFile output = new RandomAccessFile(file, "rw")) {
            output.seek(4);
            writeLittleEndianInt(output, (int) (36 + dataLength));
            output.seek(40);
            writeLittleEndianInt(output, (int) dataLength);
        }
    }

    private static void writeLittleEndianInt(RandomAccessFile output, int value) throws IOException {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
        output.write((value >> 16) & 0xff);
        output.write((value >> 24) & 0xff);
    }

    private static void writeLittleEndianShort(RandomAccessFile output, short value) throws IOException {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
    }
}
