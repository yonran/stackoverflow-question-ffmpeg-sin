
import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : /usr/include/libavformat/avio.h:7054</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class URLProtocol extends Structure {
	/** C type : const char* */
	public Pointer name;
	/** C type : url_open_callback* */
	public URLProtocol.url_open_callback url_open;
	/** C type : url_read_callback* */
	public URLProtocol.url_read_callback url_read;
	/** C type : url_write_callback* */
	public URLProtocol.url_write_callback url_write;
	/** C type : url_seek_callback* */
	public URLProtocol.url_seek_callback url_seek;
	/** C type : url_close_callback* */
	public URLProtocol.url_close_callback url_close;
	/** C type : URLProtocol* */
	public URLProtocol.ByReference next;
	/** C type : url_read_pause_callback* */
	public URLProtocol.url_read_pause_callback url_read_pause;
	/** C type : url_read_seek_callback* */
	public URLProtocol.url_read_seek_callback url_read_seek;
	/** C type : url_get_file_handle_callback* */
	public URLProtocol.url_get_file_handle_callback url_get_file_handle;
	public int priv_data_size;
	/** C type : const AVClass* */
	public AVClass.ByReference priv_data_class;
	public int flags;
	/** C type : url_check_callback* */
	public URLProtocol.url_check_callback url_check;
	/** <i>native declaration : /usr/include/libavformat/avio.h:7045</i> */
	public interface url_open_callback extends Callback {
		int apply(URLContext h, Pointer url, int flags);
	};
	/** <i>native declaration : /usr/include/libavformat/avio.h:7046</i> */
	public interface url_read_callback extends Callback {
		int apply(URLContext h, Pointer buf, int size);
	};
	/** <i>native declaration : /usr/include/libavformat/avio.h:7047</i> */
	public interface url_write_callback extends Callback {
		int apply(URLContext h, Pointer buf, int size);
	};
	/** <i>native declaration : /usr/include/libavformat/avio.h:7048</i> */
	public interface url_seek_callback extends Callback {
		long apply(URLContext h, long pos, int whence);
	};
	/** <i>native declaration : /usr/include/libavformat/avio.h:7049</i> */
	public interface url_close_callback extends Callback {
		int apply(URLContext h);
	};
	/** <i>native declaration : /usr/include/libavformat/avio.h:7050</i> */
	public interface url_read_pause_callback extends Callback {
		int apply(URLContext h, int pause);
	};
	/** <i>native declaration : /usr/include/libavformat/avio.h:7051</i> */
	public interface url_read_seek_callback extends Callback {
		long apply(URLContext h, int stream_index, long timestamp, int flags);
	};
	/** <i>native declaration : /usr/include/libavformat/avio.h:7052</i> */
	public interface url_get_file_handle_callback extends Callback {
		int apply(URLContext h);
	};
	/** <i>native declaration : /usr/include/libavformat/avio.h:7053</i> */
	public interface url_check_callback extends Callback {
		int apply(URLContext h, int mask);
	};
	public URLProtocol() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("name", "url_open", "url_read", "url_write", "url_seek", "url_close", "next", "url_read_pause", "url_read_seek", "url_get_file_handle", "priv_data_size", "priv_data_class", "flags", "url_check");
	}
	public static class ByReference extends URLProtocol implements Structure.ByReference {
		
	};
	public static class ByValue extends URLProtocol implements Structure.ByValue {
		
	};
}
