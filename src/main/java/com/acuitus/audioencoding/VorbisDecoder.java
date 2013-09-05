package com.acuitus.audioencoding;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import wraplibvorbis.VorbisencLibrary;
import wraplibvorbis.ogg_packet;
import wraplibvorbis.ogg_page;
import wraplibvorbis.ogg_stream_state;
import wraplibvorbis.ogg_sync_state;
import wraplibvorbis.vorbis_block;
import wraplibvorbis.vorbis_comment;
import wraplibvorbis.vorbis_dsp_state;
import wraplibvorbis.vorbis_info;

import com.acuitus.audioencoding.OggVorbisEncoder.OggVorbisFactory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

public class VorbisDecoder {

	int convsize = 4096;
	short[] convbuffer = new short[convsize]; /*
											 * take 8k out of the data segment,
											 * not the stack
											 */
	public final OggVorbisEncoder.VorbisLibs libs;
	public VorbisDecoder(OggVorbisEncoder.VorbisLibs libs) {
		this.libs = libs;
	}

	void main(RandomAccessFile raf) throws IOException, LineUnavailableException {
		/* sync and verify incoming physical bitstream */
		ogg_sync_state oy = new ogg_sync_state();
		oy.setAutoSynch(false);
		/** one Ogg bitstream page. Vorbis packets are inside */
		ogg_page og = new ogg_page();
		og.setAutoSynch(false);

		/** take physical pages, weld into a logical stream of packets */
		ogg_stream_state os = new ogg_stream_state();
		os.setAutoSynch(false);
		/** one raw packet of data for decode */
		ogg_packet op = new ogg_packet();
		op.setAutoSynch(false);

		/**
		 * struct that stores all the static vorbis bitstream settings
		 */
		vorbis_info vi = new vorbis_info();
		vi.setAutoSynch(false);
		/** struct that stores all the bitstream user comments */
		vorbis_comment vc = new vorbis_comment();
		vc.setAutoSynch(false);
		/** central working state for the packet->PCM decoder */
		vorbis_dsp_state vd = new vorbis_dsp_state();
		vd.setAutoSynch(false);
		/** local working space for packet->PCM decode */
		vorbis_block vb = new vorbis_block();
		vb.setAutoSynch(false);

		int bytes;

		/********** Decode setup ************/

		libs.libogg.ogg_sync_init(oy); /* Now we can read pages */

		while (true) { /* we repeat if the bitstream is chained */
			boolean eos = false;
			int i;

			/*
			 * grab some data at the head of the stream. We want the first page
			 * (which is guaranteed to be small and only contain the Vorbis
			 * stream initial header) We need the first page to get the stream
			 * serialno.
			 */

			/* submit a 4k block to libvorbis' Ogg layer */
			Pointer buffer;
			buffer = libs.libogg.ogg_sync_buffer(oy, new NativeLong(4096));
			ByteBuffer buf = buffer.getByteBuffer(0, 4096);
			bytes = raf.getChannel().read(buf);
			libs.libogg.ogg_sync_wrote(oy, new NativeLong(bytes));

			/* Get the first page. */
			if (libs.libogg.ogg_sync_pageout(oy, og) != 1) {
				/* have we simply run out of data? If so, we're done. */
				if (bytes < 4096)
					break;

				/* error case. Must not be Vorbis data */
				throw new IOException(
						"Input does not appear to be an Ogg bitstream.\n");
			}
			/* Get the serial number and set up the rest of decode. */
			/* serialno first; use it to set up a logical stream */
			libs.libogg.ogg_stream_init(os, libs.libogg.ogg_page_serialno(og));

			/*
			 * extract the initial header from the first page and verify that
			 * the Ogg bitstream is in fact Vorbis data
			 */

			/*
			 * I handle the initial header first instead of just having the code
			 * read all three Vorbis headers at once because reading the initial
			 * header is an easy way to identify a Vorbis bitstream and it's
			 * useful to see that functionality seperated out.
			 */

			libs.libvorbis.vorbis_info_init(vi);
			libs.libvorbis.vorbis_comment_init(vc);
			if (libs.libogg.ogg_stream_pagein(os, og) < 0) {
				/* error; stream version mismatch perhaps */
				throw new IOException(
						"Error reading first page of Ogg bitstream data.");
			}

			if (libs.libogg.ogg_stream_packetout(os, op) != 1) {
				/* no page? must not be vorbis */
				throw new IOException("Error reading initial header packet.");
			}

			if (libs.libvorbis.vorbis_synthesis_headerin(vi, vc, op) < 0) {
				/* error case; not a vorbis header */
				throw new IOException(
						"This Ogg bitstream does not contain Vorbis audio data.");
			}

			/*
			 * At this point, we're sure we're Vorbis. We've set up the logical
			 * (Ogg) bitstream decoder. Get the comment and codebook headers and
			 * set up the Vorbis decoder
			 */

			/*
			 * The next two packets in order are the comment and codebook
			 * headers. They're likely large and may span multiple pages. Thus
			 * we read and submit data until we get our two packets, watching
			 * that no pages are missing. If a page is missing, error out;
			 * losing a header page is the only place where missing data is
			 * fatal.
			 */

			i = 0;
			while (i < 2) {
				while (i < 2) {
					int result = libs.libogg.ogg_sync_pageout(oy, og);
					if (result == 0)
						break; /* Need more data */
					/*
					 * Don't complain about missing or corrupt data yet. We'll
					 * catch it at the packet output phase
					 */
					if (result == 1) {
						libs.libogg.ogg_stream_pagein(os, og); /*
																 * we can ignore
																 * any errors
																 * here as
																 * they'll also
																 * become
																 * apparent at
																 * packetout
																 */
						while (i < 2) {
							result = libs.libogg.ogg_stream_packetout(os, op);
							if (result == 0)
								break;
							if (result < 0) {
								/*
								 * Uh oh; data at some point was corrupted or
								 * missing! We can't tolerate that in a header.
								 * Die.
								 */
								throw new IOException("Corrupt secondary header.  Exiting.");
							}
							result = libs.libvorbis.vorbis_synthesis_headerin(vi, vc, op);

							if (result < 0) {
								throw new IOException("Corrupt secondary header.  Exiting.");
							}
							i++;
						}
					}
				}
				/* no harm in not checking before adding more */
				buffer = libs.libogg.ogg_sync_buffer(oy, new NativeLong(4096));
				buf = buffer.getByteBuffer(0, 4096);
				bytes = raf.getChannel().read(buf);
				if (bytes == -1 && i < 2) {
					throw new IOException("End of file before finding all Vorbis headers!\n");
				}
				libs.libogg.ogg_sync_wrote(oy, new NativeLong(bytes));
			}

			/*
			 * Throw the comments plus a few lines about the bitstream we're
			 * decoding
			 */
			{
				vi.read();
				vc.read();
				Pointer[] commentPointers = vc.user_comments.getPointerArray(0);
				for (Pointer p : commentPointers) {
					String comment = p.getString(0);
					System.out.println(comment);
				}
				System.out.format("Bitstream is %d channel, %dHz\n",
						vi.channels, vi.rate.longValue());
				System.out.format("Encoded by: %s\n\n", vc.vendor.getString(0));
			}
			convsize = 4096 / vi.channels;
			ByteOrder byteOrder = ByteOrder.nativeOrder();
			SourceDataLine sdl = AudioSystem.getSourceDataLine(new AudioFormat(Encoding.PCM_SIGNED, vi.rate.floatValue(), Short.SIZE, vi.channels, Short.SIZE/8 * vi.channels, vi.rate.floatValue(), byteOrder == ByteOrder.BIG_ENDIAN));
			ByteBuffer out = ByteBuffer.allocate(convsize * (Short.SIZE / 8) * vi.channels);
			out.order(byteOrder);

			sdl.open();
			sdl.start();
			if (false)
			for (int k = 0; k< vi.rate.longValue(); k++) {
				out.putShort((short) (30000 * Math.sin(k * 440. / vi.rate.longValue())));
				out.putShort((short) (30000 * Math.sin(k * 2*Math.PI * 440. / vi.rate.longValue())));
				if (!out.hasRemaining()) {
					sdl.write(out.array(), out.position(), out.remaining());
					out.clear();
				}
			}
			sdl.write(out.array(), out.position(), out.remaining());
			out.clear();

			/*
			 * OK, got and parsed all three headers. Initialize the Vorbis
			 * packet->PCM decoder.
			 */
			if (libs.libvorbis.vorbis_synthesis_init(vd, vi) == 0) { /*
																	 * central
																	 * decode
																	 * state
																	 */
				libs.libvorbis.vorbis_block_init(vd, vb); /*
														 * local state for most
														 * of the decode so
														 * multiple block
														 * decodes can proceed
														 * in parallel. We could
														 * init multiple
														 * vorbis_block
														 * structures for vd
														 * here
														 */

				/* The rest is just a straight decode loop until end of stream */
				while (!eos) {
					while (!eos) {
						int result = libs.libogg.ogg_sync_pageout(oy, og);
						if (result == 0)
							break; /* need more data */
						if (result < 0) { /*
										 * missing or corrupt data at this page
										 * position
										 */
							System.out
									.println("Corrupt or missing data in bitstream; continuing...");
						} else {
							libs.libogg.ogg_stream_pagein(os, og); /*
																	 * can
																	 * safely
																	 * ignore
																	 * errors at
																	 * this
																	 * point
																	 */
							while (true) {
								result = libs.libogg.ogg_stream_packetout(os, op);

								if (result == 0)
									break; /* need more data */
								if (result < 0) { /*
												 * missing or corrupt data at
												 * this page position
												 */
									/*
									 * no reason to complain; already complained
									 * above
									 */
								} else {
									/* we have a packet. Decode it */
									int samples;

									if (libs.libvorbis.vorbis_synthesis(vb, op) == 0) /*
																					 * test
																					 * for
																					 * success
																					 * !
																					 */
										libs.libvorbis.vorbis_synthesis_blockin(vd, vb);
									/*
									 * 
									 * *pcm is a multichannel float vector. In
									 * stereo, for example, pcm[0] is left, and
									 * pcm[1] is right. samples is the size of
									 * each channel. Convert the float values
									 * (-1.<=range<=1.) to whatever PCM format
									 * and write it out
									 */

									PointerByReference pcmReference = new PointerByReference();
									while ((samples = libs.libvorbis.vorbis_synthesis_pcmout(vd, pcmReference)) > 0) {
										int j;
										boolean clipflag = false;
										int bout = (samples < convsize ? samples : convsize);

										/*
										 * convert floats to 16 bit signed ints
										 * (host order) and interleave
										 */
										Pointer[] pcmPointers = pcmReference.getValue().getPointerArray(0, vi.channels);
										for (j = 0; j < bout; j++) {
											for (i = 0; i < vi.channels; i++) {
												Pointer mono = pcmPointers[i];
												float floatVal = mono.getFloat((Float.SIZE / 8) * j);
												int val = (int) (floatVal * 32767.f + .5f);
												/* optional dither */
												// int val=mono[j]*32767.f+drand48()-0.5f;
												/*
												 * might as well guard against
												 * clipping
												 */
												if (val > 32767) {
													val = 32767;
													clipflag = true;
												}
												if (val < -32768) {
													val = -32768;
													clipflag = true;
												}
												out.putShort((short) val);
											}
										}

										if (clipflag) {
											vd.readField("sequence");
											System.out.format("Clipping in frame %d\n", vd.sequence);
										}

										// fwrite(convbuffer,2*vi.channels,bout,stdout);
										if (false)
											System.out.write(out.array());
										out.flip();
										sdl.write(out.array(), out.position(), out.remaining());
										out.clear();

										libs.libvorbis.vorbis_synthesis_read(vd, bout); /*
															 * tell libvorbis
															 * how many samples
															 * we actually
															 * consumed
															 */
									}
								}
							}
							if (0 != libs.libogg.ogg_page_eos(og))
								eos = true;
						}
					}
					if (!eos) {
						buffer = libs.libogg.ogg_sync_buffer(oy, new NativeLong(4096));
						buf = buffer.getByteBuffer(0, 4096);
						bytes = raf.getChannel().read(buf);
						libs.libogg.ogg_sync_wrote(oy, new NativeLong(bytes < 0 ? 0 : bytes));
						if (bytes == 0 || bytes == -1)
							eos = true;
					}
				}

				/*
				 * ogg_page and ogg_packet structs always point to storage in
				 * libvorbis. They're never freed or manipulated directly
				 */

				libs.libvorbis.vorbis_block_clear(vb);
				libs.libvorbis.vorbis_dsp_clear(vd);
			} else {
				System.out
						.println("Error: Corrupt header during playback initialization.\n");
			}

			/*
			 * clean up this logical bitstream; before exit we see if we're
			 * followed by another [chained]
			 */

			libs.libogg.ogg_stream_clear(os);
			libs.libvorbis.vorbis_comment_clear(vc);
			libs.libvorbis.vorbis_info_clear(vi); /* must be called last */
		}

		/* OK, clean up the framer */
		libs.libogg.ogg_sync_clear(oy);

		System.out.println("Done.");
		return;
	}
	public static void main(String[] argv) throws IOException, LineUnavailableException, InterruptedException {
		RandomAccessFile raf = new RandomAccessFile("/tmp/recording-2013-09-05-0342-2.ogg", "r");
		OggVorbisFactory oggVorbisFactory = OggVorbisEncoder.OggVorbisFactory.create();
		new VorbisDecoder(oggVorbisFactory.libs).main(raf);
		Thread.sleep(10000);
	}
}
