package wraplibvorbis;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.sun.jna.Callback;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:19</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class ov_callbacks extends Structure {
	/** C type : read_func_callback* */
	public ov_callbacks.read_func_callback read_func;
	/** C type : seek_func_callback* */
	public ov_callbacks.seek_func_callback seek_func;
	/** C type : close_func_callback* */
	public ov_callbacks.close_func_callback close_func;
	/** C type : tell_func_callback* */
	public ov_callbacks.tell_func_callback tell_func;
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:15</i> */
	public interface read_func_callback extends Callback {
		NativeSize apply(Pointer ptr, NativeSize size, NativeSize nmemb, Pointer datasource);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:16</i> */
	public interface seek_func_callback extends Callback {
		int apply(Pointer datasource, long offset, int whence);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:17</i> */
	public interface close_func_callback extends Callback {
		int apply(Pointer datasource);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:18</i> */
	public interface tell_func_callback extends Callback {
		NativeLong apply(Pointer datasource);
	};
	public ov_callbacks() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("read_func", "seek_func", "close_func", "tell_func");
	}
	/**
	 * @param read_func C type : read_func_callback*<br>
	 * @param seek_func C type : seek_func_callback*<br>
	 * @param close_func C type : close_func_callback*<br>
	 * @param tell_func C type : tell_func_callback*
	 */
	public ov_callbacks(ov_callbacks.read_func_callback read_func, ov_callbacks.seek_func_callback seek_func, ov_callbacks.close_func_callback close_func, ov_callbacks.tell_func_callback tell_func) {
		super();
		this.read_func = read_func;
		this.seek_func = seek_func;
		this.close_func = close_func;
		this.tell_func = tell_func;
	}
	public static class ByReference extends ov_callbacks implements Structure.ByReference {
		
	};
	public static class ByValue extends ov_callbacks implements Structure.ByValue {
		
	};
}