/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.challengetask.audio;

import com.sun.jna.Native;
import com.sun.jna.ptr.PointerByReference;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import net.tomp2p.opuswrapper.Opus;


public class OpusSoundTest {
	static int BITRATE =   16000;
	static int BIT_DEPTH =      16;
	static int FRAME_LENGTH = 5;
	static int FRAME_SIZE = (BITRATE * BIT_DEPTH * FRAME_LENGTH)/(1000*8);
        
        private boolean running = false;
	private Thread recordThread;
        private Thread playThread;
        
	private TargetDataLine microphone;
	private SourceDataLine speaker;
	
	private IntBuffer error;
	private PointerByReference opusEncoder;
	private PointerByReference opusDecoder;

        // Load Opus Library
	static {
		try {
			System.loadLibrary("opus");
		} catch (UnsatisfiedLinkError e1) {
			try {
				File f = Native.extractFromResourcePath("opus");
				System.load(f.getAbsolutePath());
			} catch (Exception e2) {
				e1.printStackTrace();
				e2.printStackTrace();
			}
		}
	}
        
        public void stop() {
            running = false;
        }

	public void start() throws LineUnavailableException {
		final AudioFormat format = new AudioFormat(BITRATE, BIT_DEPTH, 1, true, true);
		final BlockingQueue<byte[]> audioBuffer = new LinkedBlockingQueue<>();

		// Microphone line
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
		if (!AudioSystem.isLineSupported(info)) {
			throw new LineUnavailableException("not supported");
		}
		microphone = AudioSystem.getTargetDataLine(format);
		microphone.open(format);
                microphone.start();
                
		// Playback line
		speaker = AudioSystem.getSourceDataLine(format);
		speaker.open(format);
		speaker.start();
		
		error = IntBuffer.allocate(4);
		opusEncoder = Opus.INSTANCE.opus_encoder_create(BITRATE, 1,
				Opus.OPUS_APPLICATION_RESTRICTED_LOWDELAY, error);
		opusDecoder = Opus.INSTANCE.opus_decoder_create(BITRATE, 1, error);

		// Thread that records audio
		class RecordThread implements Runnable {
			@Override
			public void run() {
				int i=0;
				while(running) {
					try {
						byte dataFromMic[] = recordFromMicrophone(format);
						if (dataFromMic != null) {
							audioBuffer.put(dataFromMic);
						} else {
							Thread.sleep(FRAME_LENGTH/2);
						}
						
					} catch (LineUnavailableException | InterruptedException e) {
						e.printStackTrace();
					}

				}
                                microphone.close();
			}
		}


		// Thread that plays audio
		class PlayThread implements Runnable {
			@Override
			public void run(){
                                int errors = 0;
                                while (running) {
					try {
                                                int size = audioBuffer.size();
                                                if(size > 20) {
                                                    audioBuffer.clear();
                                                    errors++;
                                                    System.out.println("Deleted " + size);
                                                    //if (errors>2) running = false;
                                                }
						byte packet[] = audioBuffer.poll();
						if (packet != null) {
                                                        //System.out.println(audioBuffer.size());
                                                        ShortBuffer decodedFromNetwork = decode(packet);
							playBack(format, decodedFromNetwork);
						} else {
							Thread.sleep(FRAME_LENGTH/4);
						}
					} catch (LineUnavailableException | InterruptedException e) {
                                                System.out.println("error");
						e.printStackTrace();
					}					
				}
                                speaker.close();
			}
		}

		// Start threads
		recordThread = new Thread(new PlayThread());
		playThread = new Thread(new RecordThread());

		recordThread.start();
		playThread.start();

                running = true;
	}

	private ShortBuffer decode(byte packet[]) {
		ShortBuffer shortBuffer = ShortBuffer.allocate(1024 * 1024);


		int decoded = Opus.INSTANCE.opus_decode(opusDecoder, packet, packet.length,
				shortBuffer, FRAME_SIZE, 0);
		shortBuffer.position(shortBuffer.position() + decoded);
		shortBuffer.flip();

		return shortBuffer;
	}


	private void playBack(AudioFormat format, ShortBuffer shortBuffer) throws LineUnavailableException {
		short[] shortAudioBuffer = new short[shortBuffer.remaining()];
		shortBuffer.get(shortAudioBuffer);
		byte[] audio = ShortToByte_Twiddle_Method(shortAudioBuffer);
		speaker.write(audio, 0, audio.length);
	}

	private byte[] recordFromMicrophone(AudioFormat format)
			throws LineUnavailableException {
		
		byte[] data = new byte[FRAME_SIZE];
		ShortBuffer shortBuffer = ShortBuffer.allocate(FRAME_SIZE/2);
		int numBytesRead;
		// Read the next chunk of data from the TargetDataLine.
		if (microphone.available() < data.length) {
			return null;
		}
		numBytesRead = microphone.read(data, 0, data.length);

		// Save this chunk of data.
		for (int i = 0; i < numBytesRead; i += 2) {
			int b1 = data[i + 1] & 0xff;
			int b2 = data[i] << 8;
			shortBuffer.put((short) (b1 | b2));
		}
		shortBuffer.flip();
		
		// Encoding
		int read = 0;
		ByteBuffer dataBuffer = ByteBuffer.allocate(FRAME_SIZE);
		//int toRead = Math.min(shortBuffer.remaining(), dataBuffer.remaining());
		int toRead = shortBuffer.capacity();
		read = Opus.INSTANCE.opus_encode(opusEncoder, shortBuffer, FRAME_SIZE/2, dataBuffer, toRead);
		// System.err.println("read: "+read);
		dataBuffer.position(dataBuffer.position() + read);
		dataBuffer.flip();
		
		byte packet[] = new byte[read];
		dataBuffer.get(packet);
		dataBuffer.flip();
		
		return packet;
		
	}

	private byte[] ShortToByte_Twiddle_Method(final short[] input) {
		final int len = input.length;
		final byte[] buffer = new byte[len * 2];
		for (int i = 0; i < len; i++) {
			buffer[(i * 2) + 1] = (byte) (input[i]);
			buffer[(i * 2)] = (byte) (input[i] >> 8);
		}
		return buffer;
	}

}
