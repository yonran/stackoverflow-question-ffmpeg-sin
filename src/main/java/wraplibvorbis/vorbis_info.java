package wraplibvorbis;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/codec.h:9</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class vorbis_info extends Structure {
	public int version;
	public int channels;
	public NativeLong rate;
	public NativeLong bitrate_upper;
	public NativeLong bitrate_nominal;
	public NativeLong bitrate_lower;
	public NativeLong bitrate_window;
	/** C type : void* */
	public Pointer codec_setup;
	public vorbis_info() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("version", "channels", "rate", "bitrate_upper", "bitrate_nominal", "bitrate_lower", "bitrate_window", "codec_setup");
	}
	/** @param codec_setup C type : void* */
	public vorbis_info(int version, int channels, NativeLong rate, NativeLong bitrate_upper, NativeLong bitrate_nominal, NativeLong bitrate_lower, NativeLong bitrate_window, Pointer codec_setup) {
		super();
		this.version = version;
		this.channels = channels;
		this.rate = rate;
		this.bitrate_upper = bitrate_upper;
		this.bitrate_nominal = bitrate_nominal;
		this.bitrate_lower = bitrate_lower;
		this.bitrate_window = bitrate_window;
		this.codec_setup = codec_setup;
	}
	public static class ByReference extends vorbis_info implements Structure.ByReference {
		
	};
	public static class ByValue extends vorbis_info implements Structure.ByValue {
		
	};
}
