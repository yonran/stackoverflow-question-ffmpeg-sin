package wraplibvorbis;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.NativeLongByReference;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:61</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class OggVorbis_File extends Structure {
	/**
	 * Pointer to a FILE *, etc.<br>
	 * C type : void*
	 */
	public Pointer datasource;
	public int seekable;
	/** C type : ogg_int64_t */
	public long offset;
	/** C type : ogg_int64_t */
	public long end;
	/** C type : ogg_sync_state */
	public ogg_sync_state oy;
	/**
	 * If the FILE handle isn't seekable (eg, a pipe), only the current<br>
	 * stream appears
	 */
	public int links;
	/** C type : ogg_int64_t* */
	public LongByReference offsets;
	/** C type : ogg_int64_t* */
	public LongByReference dataoffsets;
	/** C type : long* */
	public NativeLongByReference serialnos;
	/**
	 * overloaded to maintain binary<br>
	 * compatibility; x2 size, stores both<br>
	 * beginning and end values<br>
	 * C type : ogg_int64_t*
	 */
	public LongByReference pcmlengths;
	/** C type : vorbis_info* */
	public wraplibvorbis.vorbis_info.ByReference vi;
	/** C type : vorbis_comment* */
	public wraplibvorbis.vorbis_comment.ByReference vc;
	/**
	 * Decoding working state local storage<br>
	 * C type : ogg_int64_t
	 */
	public long pcm_offset;
	public int ready_state;
	public NativeLong current_serialno;
	public int current_link;
	public double bittrack;
	public double samptrack;
	/**
	 * take physical pages, weld into a logical<br>
	 * stream of packets<br>
	 * C type : ogg_stream_state
	 */
	public ogg_stream_state os;
	/**
	 * central working state for the packet->PCM decoder<br>
	 * C type : vorbis_dsp_state
	 */
	public vorbis_dsp_state vd;
	/**
	 * local working space for packet->PCM decode<br>
	 * C type : vorbis_block
	 */
	public vorbis_block vb;
	/** C type : ov_callbacks */
	public ov_callbacks callbacks;
	public OggVorbis_File() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("datasource", "seekable", "offset", "end", "oy", "links", "offsets", "dataoffsets", "serialnos", "pcmlengths", "vi", "vc", "pcm_offset", "ready_state", "current_serialno", "current_link", "bittrack", "samptrack", "os", "vd", "vb", "callbacks");
	}
	public static class ByReference extends OggVorbis_File implements Structure.ByReference {
		
	};
	public static class ByValue extends OggVorbis_File implements Structure.ByValue {
		
	};
}