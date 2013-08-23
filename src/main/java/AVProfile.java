
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : /usr/include/libavcodec/avcodec.h:5659</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class AVProfile extends Structure {
	public int profile;
	/**
	 * < short name for the profile<br>
	 * C type : const char*
	 */
	public Pointer name;
	public AVProfile() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("profile", "name");
	}
	/**
	 * @param name < short name for the profile<br>
	 * C type : const char*
	 */
	public AVProfile(int profile, Pointer name) {
		super();
		this.profile = profile;
		this.name = name;
	}
	public static class ByReference extends AVProfile implements Structure.ByReference {
		
	};
	public static class ByValue extends AVProfile implements Structure.ByValue {
		
	};
}
