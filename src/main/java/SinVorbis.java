import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.Random;

import wraplibvorbis.OggLibrary;
import wraplibvorbis.VorbisLibrary;
import wraplibvorbis.VorbisencLibrary;
import wraplibvorbis.ogg_packet;
import wraplibvorbis.ogg_page;
import wraplibvorbis.ogg_stream_state;
import wraplibvorbis.ogg_sync_state;
import wraplibvorbis.vorbis_block;
import wraplibvorbis.vorbis_comment;
import wraplibvorbis.vorbis_dsp_state;
import wraplibvorbis.vorbis_info;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;


public class SinVorbis {
	public static class VorbisLibs {
		public final OggLibrary libogg;
		public final VorbisLibrary libvorbis;
		public final VorbisencLibrary libvorbisenc;
		public VorbisLibs(OggLibrary libogg, VorbisLibrary libvorbis, VorbisencLibrary libvorbisenc) {
			this.libogg = libogg;
			this.libvorbis = libvorbis;
			this.libvorbisenc = libvorbisenc;
		}
	}
	public static class OggVorbisFactory {
		private final VorbisLibs libs;
		private OggVorbisFactory(VorbisLibs libs) {
			this.libs = libs;
		}
		public static OggVorbisFactory create() throws IOException {
			String[] files = new String[] {
				"wraplibvorbis/resources/linux_x86_64/libogg.so.0.8.1",
				"wraplibvorbis/resources/linux_x86_64/libvorbisfile.so.3.3.5",
				"wraplibvorbis/resources/linux_x86_64/libvorbisenc.so.2.0.9",
				"wraplibvorbis/resources/linux_x86_64/libvorbis.so.0.4.6",
			};
			VorbisLibrary libvorbis = null;
			OggLibrary libogg = null;
			VorbisencLibrary libvorbisenc = null;
			for (String resourcePath: files) {
				String basename = new File(resourcePath).getName();
				int firstDotPos = basename.indexOf('.');
				String beforeDot = basename.substring(0, firstDotPos);
				File tempFile = File.createTempFile(beforeDot, ".so");
				tempFile.deleteOnExit();
				try (
					InputStream resourceStream = Sin.class.getResourceAsStream(resourcePath);
					OutputStream tempOutput = new FileOutputStream(tempFile);
					) {
					copy(resourceStream, tempOutput);
				}
				Runtime.getRuntime().load(tempFile.getPath());
//					NativeLibrary.getInstance(tempFile.getPath());
				if (resourcePath == "wraplibvorbis/resources/linux_x86_64/libvorbis.so.0.4.6") {
					libvorbis = (VorbisLibrary) Native.loadLibrary(tempFile.getPath(), VorbisLibrary.class);
				} else if (resourcePath == "wraplibvorbis/resources/linux_x86_64/libogg.so.0.8.1") {
					libogg = (OggLibrary) Native.loadLibrary(tempFile.getPath(), OggLibrary.class);
				} else if (resourcePath == "wraplibvorbis/resources/linux_x86_64/libvorbisenc.so.2.0.9") {
					libvorbisenc = (VorbisencLibrary) Native.loadLibrary(tempFile.getPath(), VorbisencLibrary.class);
				}
			}
			return new OggVorbisFactory(new VorbisLibs(libogg, libvorbis, libvorbisenc));
		}
		/**
		 * 
		 * @param out
		 * @param sampleRate samples/s e.g. 44100
		 * @param numChannels e.g. stereo=2
		 * @param baseQuality .4 = 128kbps; .1=80kbps
		 * @return
		 * @throws IOException 
		 */
		public OggVorbisEncoder createEncoder(WritableByteChannel out, int sampleRate, int numChannels, float baseQuality) throws IOException {
			// see http://svn.xiph.org/trunk/vorbis/examples/encoder_example.c
			ogg_stream_state os = new ogg_stream_state(); os.setAutoSynch(false);

			/********* Encode setup *****/
			/** static vorbis bitstream settings */
			vorbis_info vi = new vorbis_info(); vi.setAutoSynch(false);
			libs.libvorbis.vorbis_info_init(vi);
			// Encoder could be initialized in several different ways using vorbis_encode_init, vorbis_encode_ctl. See example.
			int overr;
			if (0 != (overr = libs.libvorbisenc.vorbis_encode_init_vbr(vi, new NativeLong(numChannels), new NativeLong(sampleRate), baseQuality))) {
				throwOvError(overr, "vorbis_encode_init_vbr");
			}

			/** Add a comment */
			/** struct that stores all the user comments */
			vorbis_comment vc = new vorbis_comment(); vc.setAutoSynch(false);
			libs.libvorbis.vorbis_comment_init(vc);
			libs.libvorbis.vorbis_comment_add_tag(vc, "ENCODER", SinVorbis.class.getName());
			libs.libvorbis.vorbis_comment_add_tag(vc, "hello", "world");

			/** Setup the analysis state and auxiliary encoding storage */
			/** central working state for the packet->PCM decoder */
			vorbis_dsp_state vd = new vorbis_dsp_state(); vd.setAutoSynch(false);
			/** local working space for packet->PCM decode */
			vorbis_block vb = new vorbis_block(); vb.setAutoSynch(false);
			libs.libvorbis.vorbis_analysis_init(vd, vi);
			libs.libvorbis.vorbis_block_init(vd, vb);

			/** set up our packet->stream encoder */
			/* pick a random serial number; that way we can more likely build chained streams just by concatenation */
			Random random = new Random(8972341);
			int serialno = random.nextInt();
			if (0 != libs.libogg.ogg_stream_init(os, serialno)) {
				throw new IllegalStateException();
			}

			/*
			 * Vorbis streams begin with three headers; the initial header (with
			 * most of the codec setup parameters) which is mandated by the Ogg
			 * bitstream spec. The second header holds any comment fields. The third
			 * header holds the bitstream codebook. We merely need to make the
			 * headers, then pass them to libvorbis one at a time; libvorbis handles
			 * the additional Ogg bitstream constraints
			 */
			ogg_packet header = new ogg_packet(); header.setAutoSynch(false);
			ogg_packet header_comm = new ogg_packet(); header_comm.setAutoSynch(false);
			ogg_packet header_code = new ogg_packet(); header_code.setAutoSynch(false);
			libs.libvorbis.vorbis_analysis_headerout(vd, vc, header, header_comm, header_code);
			libs.libogg.ogg_stream_packetin(os, header);  // automatically placed in its own page
			libs.libogg.ogg_stream_packetin(os, header_comm);
			libs.libogg.ogg_stream_packetin(os, header_code);
			/** one Ogg bitstream page that will contain Vorbis packets */
			ogg_page og = new ogg_page(); og.setAutoSynch(false);
			/* This ensures the actual audio data will start on a new page, as per spec.*/
			while (0 != libs.libogg.ogg_stream_flush(os, og)) {
				og.read();
				ByteBuffer headerBuffer = og.header.getByteBuffer(0, og.header_len.longValue());
				ByteBuffer bodyBuffer = og.body.getByteBuffer(0, og.body_len.longValue());
				out.write(headerBuffer);
				out.write(bodyBuffer);
			}
			return new OggVorbisEncoder(libs, os, vb, vd, vc, vi, out);
		}
	}
	public static class OggVorbisEncoder implements AutoCloseable {
		private final WritableByteChannel out;
		private final ogg_stream_state os;
		private final vorbis_block vb;
		private final vorbis_dsp_state vd;
		private final vorbis_comment vc;
		private final vorbis_info vi;
		/** Out param of ogg_stream_pageout. Reused for speed. */
		private final ogg_page og;
		/** Out param of vorbis_bitrate_flushpacket. Reused for speed. */
		private final ogg_packet op;
		long audio_time = 0;  // in (1/bitrate) s
		private VorbisLibs libs;
		private OggVorbisEncoder(VorbisLibs libs, ogg_stream_state os, vorbis_block vb, vorbis_dsp_state vd, vorbis_comment vc, vorbis_info vi, WritableByteChannel out) {
			this.libs = Objects.requireNonNull(libs);
			this.os = Objects.requireNonNull(os);
			this.vb = Objects.requireNonNull(vb);
			this.vd = Objects.requireNonNull(vd);
			this.vc = Objects.requireNonNull(vc);
			this.vi = Objects.requireNonNull(vi);
			this.out = Objects.requireNonNull(out);
			/** one Ogg bitstream page that will contain Vorbis packets */
			this.og = new ogg_page(); og.setAutoSynch(false);
			/** One raw packet of data */
			this.op = new ogg_packet(); op.setAutoSynch(false);
		}
		public void write(FloatBuffer... floatBuffers) throws IOException {
			// Allocate (non-interleaved) buffers float[2][1024]
			int numSamples = floatBuffers[0].remaining();
			if (numSamples == 0)
				return;
			Pointer buffers = libs.libvorbis.vorbis_analysis_buffer(vd, numSamples).getPointer();
			for (int channelId = 0; channelId < floatBuffers.length; channelId++) {
				FloatBuffer channel = floatBuffers[channelId];
				Pointer channelPointer = buffers.getPointer(Pointer.SIZE * channelId);
				for (int i = 0; i < numSamples; i++) {
					float sample = channel.get();
					channelPointer.setFloat(i * (Float.SIZE/8), sample);
				}
			}
			audio_time += numSamples;
			int overr;
			/* tell the library how much we actually submitted */
			if ((overr = libs.libvorbis.vorbis_analysis_wrote(vd, numSamples)) < 0) {
				throwOvError(overr, "vorbis_analysis_wrote");
			}
			blockout();
		}
		private void blockout() throws IOException {
			int overr;
			/*
			 * vorbis does some data preanalysis, then divvies up blocks for more
			 * involved (potentially parallel) processing. Get a single block for
			 * encoding now
			 */
			while (1 == (overr = libs.libvorbis.vorbis_analysis_blockout(vd, vb))) {
				/* analysis, assume we want to use bitrate management */
				if ((overr = libs.libvorbis.vorbis_analysis(vb, null)) < 0) {
					throw new IllegalStateException();
				}
				if ((overr = libs.libvorbis.vorbis_bitrate_addblock(vb)) < 0) {
					throw new IllegalStateException();
				}
				while (1 == (overr = libs.libvorbis.vorbis_bitrate_flushpacket(vd, op))) {
					/* weld the packet into the bitstream */
					libs.libogg.ogg_stream_packetin(os, op);
					while (0 != libs.libogg.ogg_stream_pageout(os, og)) {
						/* write out pages (if any) */
						og.read();
						ByteBuffer headerBuffer = og.header.getByteBuffer(0, og.header_len.longValue());
						ByteBuffer bodyBuffer = og.body.getByteBuffer(0, og.body_len.longValue());
						out.write(headerBuffer);
						out.write(bodyBuffer);
					}
				}
				if (overr < 0) {
					throw new IllegalStateException();
				}
			}
			if (overr < 0) {
				throw new IllegalStateException();
			}
		}
		@Override public void close() throws IOException {
			int overr;
			// submit an empty buffer to signal end of input.
			if ((overr = libs.libvorbis.vorbis_analysis_wrote(vd, 0)) < 0) {
				throwOvError(overr, "vorbis_analysis_wrote");
			}
			blockout();
			/* clean up and exit.  vorbis_info_clear() must be called last */
			libs.libogg.ogg_stream_clear(os);
			libs.libvorbis.vorbis_block_clear(vb);
			libs.libvorbis.vorbis_dsp_clear(vd);
			libs.libvorbis.vorbis_comment_clear(vc);
			libs.libvorbis.vorbis_info_clear(vi);
			/* ogg_page and ogg_packet structs always point to storage in libvorbis.
			 * They're never freed or manipulated directly
			 */
			out.close();
		}
	}
	private static void copy(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[2048];
		int n;
		while (-1 != (n = is.read(buf))) {
			os.write(buf, 0, n);
		}
	}
	public static void main(String[] argv) throws IOException {
		File f = new File("sinvorbis.ogg");
		OggVorbisFactory factory = OggVorbisFactory.create();
		int sampleRate = 44100;
		try (
			FileOutputStream out = new FileOutputStream(f);
			OggVorbisEncoder encoder = factory.createEncoder(out.getChannel(), sampleRate, 2, .1f)
			) {
			int total_samples = 30 * sampleRate;
			FloatBuffer left = FloatBuffer.allocate(1024), right = FloatBuffer.allocate(1024);
			int audio_time = 0;
			for (int i = 0; i < total_samples; i++) {
				double x = 440*2*Math.PI * audio_time / sampleRate;
				float y = (float) (.3*Math.sin(x));
				left.put(y);
				right.put(y);
				audio_time++;
				if (! left.hasRemaining()) {
					left.flip();
					right.flip();
					encoder.write(left, right);
					left.clear();
					right.clear();
				}
			}
			if (left.position() != 0) {
				encoder.write(left, right);
				left.clear();
				right.clear();
			}
		}

	}
	public void read() throws FileNotFoundException, IOException {
		VorbisLibs libs = null;
		
		/** sync and verify incoming physical bitstream */
		ogg_sync_state oy = new ogg_sync_state(); oy.setAutoSynch(false);
		/** take physical pages, weld into a logical stream of packets */
		ogg_stream_state os = new ogg_stream_state(); os.setAutoSynch(false);
		/** one Ogg bitstream page. Vorbis packets are inside */
		ogg_page og = new ogg_page(); og.setAutoSynch(false);
		/** one raw packet of data for decode */
		ogg_packet op = new ogg_packet(); op.setAutoSynch(false);

		/** struct that stores all the static vorbis bitstream settings */
		vorbis_info vi = new vorbis_info(); vi.setAutoSynch(false);
		libs.libvorbis.vorbis_info_init(vi);

		/** struct that stores all the bitstream user comments */
		vorbis_comment vc = new vorbis_comment(); vc.setAutoSynch(false);
		libs.libvorbis.vorbis_comment_init(vc);
		
		/** central working state for the packet->PCM decoder */
		vorbis_dsp_state vd = new vorbis_dsp_state(); vd.setAutoSynch(false);
		/** local working space for packet->PCM decode */
		vorbis_block vb = new vorbis_block(); vb.setAutoSynch(false);

		/**** Decode setup ****/
		libs.libogg.ogg_sync_init(oy);  // now we can read pages
		
		try (FileInputStream is = new FileInputStream("sinvorbis.ogg")) {
			while (true) {  // will repeat if bitstream is chained
				/* grab some data at the head of the stream. We want the first page
				 * (which is guaranteed to be small and only contain the Vorbis
				 * stream initial header) We need the first page to get the stream
				 * serialno.
				 */
				/* submit a 4k block to libvorbis' Ogg layer */
				Pointer buffer = libs.libogg.ogg_sync_buffer(oy, new NativeLong(4096));
				ByteBuffer bufferBuffer = buffer.getByteBuffer(0, 4096);
				is.getChannel().read(bufferBuffer);
				int bytes = bufferBuffer.position();
				libs.libogg.ogg_sync_wrote(oy, new NativeLong(bytes));
				
				// get the first page
				if (1 != libs.libogg.ogg_sync_pageout(oy, og)) {
					/* have we simply run out of data?  If so, we're done. */
					if (bytes < 4096)
						break;
					/* error case.  Must not be Vorbis data */
					throw new IllegalStateException("Input does not appear to be an Ogg bitstream.");
				}
				/* Get the serial number and set up the rest of decode. */
				/* serialno first; use it to set up a logical stream */
				libs.libogg.ogg_stream_init(os, libs.libogg.ogg_page_serialno(og));
				/* extract the initial header from the first page and verify
				 * that the Ogg bitstream is in fact Vorbis data
				 */
				/* I handle the initial header first instead of just having the
				 * code read all three Vorbis headers at once because reading
				 * the initial header is an easy way to identify a Vorbis
				 * bitstream and it's useful to see that functionality seperated
				 * out.
				 */
				/* I handle the initial header first instead of just having the code
			       read all three Vorbis headers at once because reading the initial
			       header is an easy way to identify a Vorbis bitstream and it's
			       useful to see that functionality seperated out. */
			    
				libs.libvorbis.vorbis_info_init(vi);
				libs.libvorbis.vorbis_comment_init(vc);
			    if (libs.libogg.ogg_stream_pagein(os,og)<0){ 
			      /* error; stream version mismatch perhaps */
			      throw new IllegalStateException("Error reading first page of Ogg bitstream data.");
			    }
			    
			    if (libs.libogg.ogg_stream_packetout(os, op)!=1){ 
			      /* no page? must not be vorbis */
			    	throw new IllegalStateException("Error reading initial header packet.");
			    }
			    
			    if(libs.libvorbis.vorbis_synthesis_headerin(vi,vc,op)<0){ 
			      /* error case; not a vorbis header */
			    	throw new IllegalStateException("This Ogg bitstream does not contain Vorbis audio data.");
			    }
			    
			    /* At this point, we're sure we're Vorbis. We've set up the logical
			       (Ogg) bitstream decoder. Get the comment and codebook headers and
			       set up the Vorbis decoder */
			    
			    /* The next two packets in order are the comment and codebook headers.
			       They're likely large and may span multiple pages. Thus we read
			       and submit data until we get our two packets, watching that no
			       pages are missing. If a page is missing, error out; losing a
			       header page is the only place where missing data is fatal. */
			    
			    int i = 0;
			    while(i<2){
			      while(i<2){
			        int result=libs.libogg.ogg_sync_pageout(oy,og);
			        if(result==0)break; /* Need more data */
			        /* Don't complain about missing or corrupt data yet. We'll
			           catch it at the packet output phase */
			        if(result==1){
			        	libs.libogg.ogg_stream_pagein(os,og); /* we can ignore any errors here
			                                         as they'll also become apparent
			                                         at packetout */
			          while(i<2){
			            result=libs.libogg.ogg_stream_packetout(os,op);
			            if(result==0)break;
			            if(result<0){
			              /* Uh oh; data at some point was corrupted or missing!
			                 We can't tolerate that in a header.  Die. */
			            	throw new IllegalStateException("Corrupt secondary header.  Exiting.");
			            }
			            result=libs.libvorbis.vorbis_synthesis_headerin(vi,vc,op);
			            if(result<0){
			            	throw new IllegalStateException("Corrupt secondary header.  Exiting.");
			            }
			            i++;
			          }
			        }
			      }
			      /* no harm in not checking before adding more */
			      buffer=libs.libogg.ogg_sync_buffer(oy,new NativeLong(4096));
			      bufferBuffer = buffer.getByteBuffer(0, 4096);
			      is.getChannel().read(bufferBuffer);
			      bytes = bufferBuffer.position();
			      if(bytes==0 && i<2){
			        throw new IllegalStateException("End of file before finding all Vorbis headers!");
			      }
			      libs.libogg.ogg_sync_wrote(oy,new NativeLong(bytes));
			    }
			    
			    /* Throw the comments plus a few lines about the bitstream we're
			       decoding */
			    {
			      Pointer ptr = vc.user_comments;
			      for (Pointer commentPtr: ptr.getPointerArray(0)) {
			    	  String comment = commentPtr.getString(0);
			    	  System.out.println(comment);
			      }
			      vi.read();
			      System.out.format("\nBitstream is %d channel, %ldHz\n",vi.channels,vi.rate);
			      System.out.format("Encoded by: %s\n\n",vc.vendor);
			    }
			    
			    int convsize = 4096/vi.channels;

			    /* OK, got and parsed all three headers. Initialize the Vorbis
			       packet->PCM decoder. */
			    if(libs.libvorbis.vorbis_synthesis_init(vd,vi)==0){ /* central decode state */
			    	libs.libvorbis.vorbis_block_init(vd,vb);          /* local state for most of the decode
			                                              so multiple block decodes can
			                                              proceed in parallel. We could init
			                                              multiple vorbis_block structures
			                                              for vd here */
			      
			      /* The rest is just a straight decode loop until end of stream */
			      boolean eos = false;
				while(!eos ){
			        while(!eos){
			          int result=libs.libogg.ogg_sync_pageout(oy,og);
			          if(result==0)break; /* need more data */
			          if(result<0){ /* missing or corrupt data at this page position */
			            System.out.format("Corrupt or missing data in bitstream; continuing...\n");
			          }else{
			        	  libs.libogg.ogg_stream_pagein(os,og); /* can safely ignore errors at
			                                           this point */
			            while(true){
			              result=libs.libogg.ogg_stream_packetout(os,op);
			              
			              if(result==0)break; /* need more data */
			              if(result<0){ /* missing or corrupt data at this page position */
			                /* no reason to complain; already complained above */
			              }else{
			                /* we have a packet.  Decode it */
			                int samples;
			                
			                if(libs.libvorbis.vorbis_synthesis(vb,op)==0) /* test for success! */
			                	libs.libvorbis.vorbis_synthesis_blockin(vd,vb);
			                /* 
			                   
			                **pcm is a multichannel float vector.  In stereo, for
			                example, pcm[0] is left, and pcm[1] is right.  samples is
			                the size of each channel.  Convert the float values
			                (-1.<=range<=1.) to whatever PCM format and write it out */
			                
			                PointerByReference pcm = new PointerByReference();  // float**
			                while((samples=libs.libvorbis.vorbis_synthesis_pcmout(vd,pcm))>0){
			                  int j;
			                  boolean clipflag=false;
			                  int bout=(samples<convsize?samples:convsize);
			                  
			                  /* convert floats to 16 bit signed ints (host order) and
			                     interleave */
			                  for(i=0;i<vi.channels;i++){
			                    ogg_int16_t *ptr=convbuffer+i;
			                    float  *mono=pcm[i];
			                    for(j=0;j<bout;j++){
			#if 1
			                      int val=floor(mono[j]*32767.f+.5f);
			#else /* optional dither */
			                      int val=mono[j]*32767.f+drand48()-0.5f;
			#endif
			                      /* might as well guard against clipping */
			                      if(val>32767){
			                        val=32767;
			                        clipflag=1;
			                      }
			                      if(val<-32768){
			                        val=-32768;
			                        clipflag=1;
			                      }
			                      *ptr=val;
			                      ptr+=vi.channels;
			                    }
			                  }
			                  
			                  if(clipflag)
			                    System.err.format("Clipping in frame %d\n",vd.sequence);
			                  
			                  
			                  fwrite(convbuffer,2*vi.channels,bout,stdout);
			                  
			                  libs.libvorbis.vorbis_synthesis_read(vd,bout); /* tell libvorbis how
			                                                      many samples we
			                                                      actually consumed */
			                }            
			              }
			            }
			            if(0 != libs.libogg.ogg_page_eos(og))eos=true;
			          }
			        }
			        if(!eos){

				      buffer=libs.libogg.ogg_sync_buffer(oy,new NativeLong(4096));
				      bufferBuffer = buffer.getByteBuffer(0, 4096);
				      is.getChannel().read(bufferBuffer);
				      bytes = bufferBuffer.position();
				      libs.libogg.ogg_sync_wrote(oy,new NativeLong(bytes));
				      if(bytes==0)eos=true;
			        }
			      }
			      
			      /* ogg_page and ogg_packet structs always point to storage in
			         libvorbis.  They're never freed or manipulated directly */
			      
				libs.libvorbis.vorbis_block_clear(vb);
				libs.libvorbis.vorbis_dsp_clear(vd);
			    }else{
			      System.err.format("Error: Corrupt header during playback initialization.\n");
			    }

			    /* clean up this logical bitstream; before exit we see if we're
			       followed by another [chained] */
			    
			    libs.libogg.ogg_stream_clear(os);
			    libs.libvorbis.vorbis_comment_clear(vc);
			    libs.libvorbis.vorbis_info_clear(vi);  /* must be called last */
			}
		} finally {
			/* OK, clean up the framer */
			libs.libogg.ogg_sync_clear(oy);
		}
		libs.libvorbis.vorbis_synthesis_headerin(vi, vc, op);
	}
	static void throwOvError(int overr, String task) {
		throw new IllegalStateException("" + overr + " while " + task);
	}
}
