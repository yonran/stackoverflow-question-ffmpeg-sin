package wrapffmpeg;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : libavutil/frame.h:285</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class AVFrame extends Structure {
	/**
	 * pointer to the picture/channel planes.<br>
	 * This might be different from the first allocated byte<br>
	 * * Some decoders access areas outside 0,0 - width,height, please<br>
	 * see avcodec_align_dimensions2(). Some filters and swscale can read<br>
	 * up to 16 bytes beyond the planes, if these filters are to be used,<br>
	 * then 16 extra bytes must be allocated.<br>
	 * C type : uint8_t*[8]
	 */
	public Pointer[] data = new Pointer[8];
	/**
	 * For video, size in bytes of each picture line.<br>
	 * For audio, size in bytes of each plane.<br>
	 * * For audio, only linesize[0] may be set. For planar audio, each channel<br>
	 * plane must be the same size.<br>
	 * * For video the linesizes should be multiplies of the CPUs alignment<br>
	 * preference, this is 16 or 32 for modern desktop CPUs.<br>
	 * Some code requires such alignment other code can be slower without<br>
	 * correct alignment, for yet other it makes no difference.<br>
	 * C type : int[8]
	 */
	public int[] linesize = new int[8];
	/**
	 * pointers to the data planes/channels.<br>
	 * * For video, this should simply point to data[].<br>
	 * * For planar audio, each channel has a separate data pointer, and<br>
	 * linesize[0] contains the size of each channel buffer.<br>
	 * For packed audio, there is just one data pointer, and linesize[0]<br>
	 * contains the total size of the buffer for all channels.<br>
	 * * Note: Both data and extended_data should always be set in a valid frame,<br>
	 * but for planar audio with more channels that can fit in data,<br>
	 * extended_data must be used in order to access all channels.<br>
	 * C type : uint8_t**
	 */
	public PointerByReference extended_data;
	/** width and height of the video frame */
	public int width;
	/** width and height of the video frame */
	public int height;
	/** number of audio samples (per channel) described by this frame */
	public int nb_samples;
	/**
	 * format of the frame, -1 if unknown or unset<br>
	 * Values correspond to enum AVPixelFormat for video frames,<br>
	 * enum AVSampleFormat for audio)
	 */
	public int format;
	/** 1 -> keyframe, 0-> not */
	public int key_frame;
	/**
	 * Picture type of the frame.<br>
	 * @see AVPictureType<br>
	 * C type : AVPictureType
	 */
	public int pict_type;
	/** C type : uint8_t*[8] */
	public Pointer[] base = new Pointer[8];
	/**
	 * Sample aspect ratio for the video frame, 0/1 if unknown/unspecified.<br>
	 * C type : AVRational
	 */
	public AVRational sample_aspect_ratio;
	/** Presentation timestamp in time_base units (time when frame should be shown to user). */
	public long pts;
	/** PTS copied from the AVPacket that was decoded to produce this frame. */
	public long pkt_pts;
	/**
	 * DTS copied from the AVPacket that triggered returning this frame. (if frame threading isnt used)<br>
	 * This is also the Presentation time of this AVFrame calculated from<br>
	 * only AVPacket.dts values without pts values.
	 */
	public long pkt_dts;
	/** picture number in bitstream order */
	public int coded_picture_number;
	/** picture number in display order */
	public int display_picture_number;
	/** quality (between 1 (good) and FF_LAMBDA_MAX (bad)) */
	public int quality;
	public int reference;
	/** C type : int8_t* */
	public Pointer qscale_table;
	public int qstride;
	public int qscale_type;
	/** C type : uint8_t* */
	public Pointer mbskip_table;
	/** C type : int16_t[2]*[2] */
	public Pointer[] motion_val = new Pointer[2];
	/** C type : uint32_t* */
	public IntByReference mb_type;
	/** C type : short* */
	public ShortByReference dct_coeff;
	/** C type : int8_t*[2] */
	public Pointer[] ref_index = new Pointer[2];
	/**
	 * for some private data of the user<br>
	 * C type : void*
	 */
	public Pointer opaque;
	/**
	 * error<br>
	 * C type : uint64_t[8]
	 */
	public long[] error = new long[8];
	public int type;
	/**
	 * When decoding, this signals how much the picture must be delayed.<br>
	 * extra_delay = repeat_pict / (2*fps)
	 */
	public int repeat_pict;
	/** The content of the picture is interlaced. */
	public int interlaced_frame;
	/** If the content is interlaced, is top field displayed first. */
	public int top_field_first;
	/** Tell user application that palette has changed from previous frame. */
	public int palette_has_changed;
	public int buffer_hints;
	/** C type : AVPanScan* */
	public wrapffmpeg.AVPanScan.ByReference pan_scan;
	/**
	 * reordered opaque 64bit (generally an integer or a double precision float<br>
	 * PTS but can be anything).<br>
	 * The user sets AVCodecContext.reordered_opaque to represent the input at<br>
	 * that time,<br>
	 * the decoder reorders values as needed and sets AVFrame.reordered_opaque<br>
	 * to exactly one of the values provided by the user through AVCodecContext.reordered_opaque<br>
	 * @deprecated in favor of pkt_pts
	 */
	public long reordered_opaque;
	/**
	 * @deprecated this field is unused<br>
	 * C type : void*
	 */
	public Pointer hwaccel_picture_private;
	/** C type : AVCodecContext* */
	public wrapffmpeg.AVCodecContext.ByReference owner;
	/** C type : void* */
	public Pointer thread_opaque;
	public byte motion_subsample_log2;
	/** Sample rate of the audio data. */
	public int sample_rate;
	/** Channel layout of the audio data. */
	public long channel_layout;
	/**
	 * AVBuffer references backing the data for this frame. If all elements of<br>
	 * this array are NULL, then this frame is not reference counted.<br>
	 * * There may be at most one AVBuffer per data plane, so for video this array<br>
	 * always contains all the references. For planar audio with more than<br>
	 * AV_NUM_DATA_POINTERS channels, there may be more buffers than can fit in<br>
	 * this array. Then the extra AVBufferRef pointers are stored in the<br>
	 * extended_buf array.<br>
	 * C type : AVBufferRef*[8]
	 */
	public wrapffmpeg.AVBufferRef.ByReference[] buf = new wrapffmpeg.AVBufferRef.ByReference[8];
	/**
	 * For planar audio which requires more than AV_NUM_DATA_POINTERS<br>
	 * AVBufferRef pointers, this array will hold all the references which<br>
	 * cannot fit into AVFrame.buf.<br>
	 * * Note that this is different from AVFrame.extended_data, which always<br>
	 * contains all the pointers. This array only contains the extra pointers,<br>
	 * which cannot fit into AVFrame.buf.<br>
	 * * This array is always allocated using av_malloc() by whoever constructs<br>
	 * the frame. It is freed in av_frame_unref().<br>
	 * C type : AVBufferRef**
	 */
	public wrapffmpeg.AVBufferRef.ByReference[] extended_buf;
	/** Number of elements in extended_buf. */
	public int nb_extended_buf;
	/** C type : AVFrameSideData** */
	public wrapffmpeg.AVFrameSideData.ByReference[] side_data;
	public int nb_side_data;
	/**
	 * frame timestamp estimated using various heuristics, in stream time base<br>
	 * Code outside libavcodec should access this field using:<br>
	 * av_frame_get_best_effort_timestamp(frame)<br>
	 * - encoding: unused<br>
	 * - decoding: set by libavcodec, read by user.
	 */
	public long best_effort_timestamp;
	/**
	 * reordered pos from the last AVPacket that has been input into the decoder<br>
	 * Code outside libavcodec should access this field using:<br>
	 * av_frame_get_pkt_pos(frame)<br>
	 * - encoding: unused<br>
	 * - decoding: Read by user.
	 */
	public long pkt_pos;
	/**
	 * duration of the corresponding packet, expressed in<br>
	 * AVStream->time_base units, 0 if unknown.<br>
	 * Code outside libavcodec should access this field using:<br>
	 * av_frame_get_pkt_duration(frame)<br>
	 * - encoding: unused<br>
	 * - decoding: Read by user.
	 */
	public long pkt_duration;
	/**
	 * metadata.<br>
	 * Code outside libavcodec should access this field using:<br>
	 * av_frame_get_metadata(frame)<br>
	 * - encoding: Set by user.<br>
	 * - decoding: Set by libavcodec.<br>
	 * C type : AVDictionary*
	 */
	public PointerByReference metadata;
	/**
	 * decode error flags of the frame, set to a combination of<br>
	 * FF_DECODE_ERROR_xxx flags if the decoder produced a frame, but there<br>
	 * were errors during the decoding.<br>
	 * Code outside libavcodec should access this field using:<br>
	 * av_frame_get_decode_error_flags(frame)<br>
	 * - encoding: unused<br>
	 * - decoding: set by libavcodec, read by user.
	 */
	public int decode_error_flags;
	/**
	 * number of audio channels, only used for audio.<br>
	 * Code outside libavcodec should access this field using:<br>
	 * av_frame_get_channels(frame)<br>
	 * - encoding: unused<br>
	 * - decoding: Read by user.
	 */
	public int channels;
	/**
	 * size of the corresponding packet containing the compressed<br>
	 * frame. It must be accessed using av_frame_get_pkt_size() and<br>
	 * av_frame_set_pkt_size().<br>
	 * It is set to a negative value if unknown.<br>
	 * - encoding: unused<br>
	 * - decoding: set by libavcodec, read by user.
	 */
	public int pkt_size;
	/**
	 * YUV colorspace type.<br>
	 * It must be accessed using av_frame_get_colorspace() and<br>
	 * av_frame_set_colorspace().<br>
	 * - encoding: Set by user<br>
	 * - decoding: Set by libavcodec<br>
	 * @see AVColorSpace<br>
	 * C type : AVColorSpace
	 */
	public int colorspace;
	/**
	 * MPEG vs JPEG YUV range.<br>
	 * It must be accessed using av_frame_get_color_range() and<br>
	 * av_frame_set_color_range().<br>
	 * - encoding: Set by user<br>
	 * - decoding: Set by libavcodec<br>
	 * @see AVColorRange<br>
	 * C type : AVColorRange
	 */
	public int color_range;
	/**
	 * Not to be accessed directly from outside libavutil<br>
	 * C type : AVBufferRef*
	 */
	public wrapffmpeg.AVBufferRef.ByReference qp_table_buf;
	public AVFrame() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("data", "linesize", "extended_data", "width", "height", "nb_samples", "format", "key_frame", "pict_type", "base", "sample_aspect_ratio", "pts", "pkt_pts", "pkt_dts", "coded_picture_number", "display_picture_number", "quality", "reference", "qscale_table", "qstride", "qscale_type", "mbskip_table", "motion_val", "mb_type", "dct_coeff", "ref_index", "opaque", "error", "type", "repeat_pict", "interlaced_frame", "top_field_first", "palette_has_changed", "buffer_hints", "pan_scan", "reordered_opaque", "hwaccel_picture_private", "owner", "thread_opaque", "motion_subsample_log2", "sample_rate", "channel_layout", "buf", "extended_buf", "nb_extended_buf", "side_data", "nb_side_data", "best_effort_timestamp", "pkt_pos", "pkt_duration", "metadata", "decode_error_flags", "channels", "pkt_size", "colorspace", "color_range", "qp_table_buf");
	}
	public static class ByReference extends AVFrame implements Structure.ByReference {
		
	};
	public static class ByValue extends AVFrame implements Structure.ByValue {
		
	};
}
