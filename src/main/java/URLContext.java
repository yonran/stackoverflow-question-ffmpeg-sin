
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : /usr/include/libavformat/avio.h:7025</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class URLContext extends Structure {
	/**
	 * < information for av_log(). Set by url_open().<br>
	 * C type : const AVClass*
	 */
	public AVClass.ByReference av_class;
	/** C type : URLProtocol* */
	public URLProtocol.ByReference prot;
	public int flags;
	/** < true if streamed (no seek possible), default = false */
	public int is_streamed;
	/** < if non zero, the stream is packetized with this max packet size */
	public int max_packet_size;
	/** C type : void* */
	public Pointer priv_data;
	/**
	 * < specified URL<br>
	 * C type : char*
	 */
	public Pointer filename;
	public int is_connected;
	/** C type : AVIOInterruptCB */
	public AVIOInterruptCB interrupt_callback;
	public URLContext() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("av_class", "prot", "flags", "is_streamed", "max_packet_size", "priv_data", "filename", "is_connected", "interrupt_callback");
	}
	/**
	 * @param av_class < information for av_log(). Set by url_open().<br>
	 * C type : const AVClass*<br>
	 * @param prot C type : URLProtocol*<br>
	 * @param is_streamed < true if streamed (no seek possible), default = false<br>
	 * @param max_packet_size < if non zero, the stream is packetized with this max packet size<br>
	 * @param priv_data C type : void*<br>
	 * @param filename < specified URL<br>
	 * C type : char*<br>
	 * @param interrupt_callback C type : AVIOInterruptCB
	 */
	public URLContext(AVClass.ByReference av_class, URLProtocol.ByReference prot, int flags, int is_streamed, int max_packet_size, Pointer priv_data, Pointer filename, int is_connected, AVIOInterruptCB interrupt_callback) {
		super();
		this.av_class = av_class;
		this.prot = prot;
		this.flags = flags;
		this.is_streamed = is_streamed;
		this.max_packet_size = max_packet_size;
		this.priv_data = priv_data;
		this.filename = filename;
		this.is_connected = is_connected;
		this.interrupt_callback = interrupt_callback;
	}
	public static class ByReference extends URLContext implements Structure.ByReference {
		
	};
	public static class ByValue extends URLContext implements Structure.ByValue {
		
	};
}
