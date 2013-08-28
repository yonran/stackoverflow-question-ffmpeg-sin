package wraplibvorbis;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/codec.h:36</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class vorbis_dsp_state extends Structure {
	public int analysisp;
	/** C type : vorbis_info* */
	public wraplibvorbis.vorbis_info.ByReference vi;
	/** C type : float** */
	public PointerByReference pcm;
	/** C type : float** */
	public PointerByReference pcmret;
	public int pcm_storage;
	public int pcm_current;
	public int pcm_returned;
	public int preextrapolate;
	public int eofflag;
	public NativeLong lW;
	public NativeLong W;
	public NativeLong nW;
	public NativeLong centerW;
	/** C type : ogg_int64_t */
	public long granulepos;
	/** C type : ogg_int64_t */
	public long sequence;
	/** C type : ogg_int64_t */
	public long glue_bits;
	/** C type : ogg_int64_t */
	public long time_bits;
	/** C type : ogg_int64_t */
	public long floor_bits;
	/** C type : ogg_int64_t */
	public long res_bits;
	/** C type : void* */
	public Pointer backend_state;
	public vorbis_dsp_state() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("analysisp", "vi", "pcm", "pcmret", "pcm_storage", "pcm_current", "pcm_returned", "preextrapolate", "eofflag", "lW", "W", "nW", "centerW", "granulepos", "sequence", "glue_bits", "time_bits", "floor_bits", "res_bits", "backend_state");
	}
	public static class ByReference extends vorbis_dsp_state implements Structure.ByReference {
		
	};
	public static class ByValue extends vorbis_dsp_state implements Structure.ByValue {
		
	};
}