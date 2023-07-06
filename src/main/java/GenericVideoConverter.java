import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVOutputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.javacpp.PointerPointer;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class GenericVideoConverter {
    // This class in meant to handle any format agnostic method, which can be reusable for any format
    // Or in fact easy to parametrize and make it generic for any format
    public AVFormatContext inputFormatCtx;
    public AVFormatContext outputFormatCtx;
    public void openVideoFile(String inFilePath){
        inputFormatCtx = new AVFormatContext(null);
        outputFormatCtx = new AVFormatContext(null); //new AVFormatContext(null);

        int ret = -1;
        ret = avformat_open_input(inputFormatCtx, inFilePath, null, null);
        if (ret < 0) { //
            throw new RuntimeException("Error opening input file: " + inFilePath);
        }

        //Read object data.
        int streamInfo = avformat_find_stream_info(inputFormatCtx, (PointerPointer)null);
        if (streamInfo < 0) {
            throw new RuntimeException("Error reading input file stream: " + inFilePath);
        }
    }


    public void createOutputContext(String outputFilePath){
        outputFormatCtx = new AVFormatContext(null);
        int ret = avformat_alloc_output_context2(outputFormatCtx, null, null, outputFilePath);
        if (ret < 0) {
            throw new RuntimeException("Could not create output context for " + outputFilePath);
        }
    }

    public void prepareStreams(){
        int ret = -1;
        for (int i = 0; i < inputFormatCtx.nb_streams(); i++) {
            AVStream inStream = inputFormatCtx.streams(i);
            AVStream outStream = avformat_new_stream(outputFormatCtx, new AVCodec(null));

            AVCodecParameters inputCodecParameters = inStream.codecpar();
            AVCodec avcDecoder = avcodec_find_decoder(inputCodecParameters.codec_id());
            AVCodecContext inputCodecs = avcodec_alloc_context3(avcDecoder);
            if (inputCodecs.codec_type() != AVMEDIA_TYPE_AUDIO &&
                    inputCodecs.codec_type() != AVMEDIA_TYPE_VIDEO) {
                continue;
            }
            ret = avcodec_parameters_copy(outStream.codecpar(), inStream.codecpar());
            if (ret < 0) {
                throw new RuntimeException("Failed to copy codec parameters");
            }
        }
    }

    public void prepareOutputContext(String outputFilePath) {
        createOutputContext(outputFilePath);
        prepareStreams();

        int ret = -1;
        AVIOContext pb = new AVIOContext(null);
        ret = avio_open(pb, outputFilePath, AVIO_FLAG_READ_WRITE);
        if (ret < 0) {
            System.out.println("Cannot open io context "+ outputFilePath);
            return;
        }
        outputFormatCtx.pb(pb);

        // Obtain the format based on the file
        AVOutputFormat fmt = av_guess_format(null, outputFilePath, null);
        outputFormatCtx.oformat(fmt);

        ret = avformat_write_header(outputFormatCtx, (PointerPointer)null);
        if (ret < 0) {
            System.out.println("Cannot write headers io context "+ outputFilePath);
        }
    }

    private void remux2(){
        int ret = -1;
        int stream_mapping_size = 0;
        stream_mapping_size = inputFormatCtx.nb_streams();
        int[] stream_mapping = new int[stream_mapping_size];

        AVPacket packet = new AVPacket();

        while(true) {
            if (av_read_frame(inputFormatCtx, packet) < 0) {break;}

            if (packet.stream_index() >= stream_mapping_size || stream_mapping[packet.stream_index()] < 0) {
                av_packet_unref(packet);
                continue;
            }

            AVRational inputTimeBase = inputFormatCtx.streams(packet.stream_index()).time_base();
            AVRational outputTimeBase = outputFormatCtx.streams(packet.stream_index()).time_base();

            av_packet_rescale_ts(packet, inputTimeBase, outputTimeBase);

            // Those values are automatically added or obtained by the inputFormatCtx(?)
            //packet.pts(av_rescale_q_rnd(packet.pts(), inputTimeBase, outputTimeBase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
            //packet.dts(av_rescale_q_rnd(packet.dts(), inputTimeBase, outputTimeBase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
            //packet.duration(av_rescale_q(packet.duration(), inputTimeBase, outputTimeBase));
            //packet.pos(-1);

            av_interleaved_write_frame(outputFormatCtx, packet);

        }

        //write packet to file!
        av_write_trailer(outputFormatCtx);

    }

    public void convert(String inputFilePath, String outputFilePath){
        openVideoFile(inputFilePath);
        prepareOutputContext(outputFilePath);
        remux2();
        System.out.println(inputFilePath + " Converted to " + outputFilePath);
    }

    public void convertTo(String inputFilePath, String extFormat) {
        convert(inputFilePath, inputFilePath.substring(0, inputFilePath.lastIndexOf('.')+1)+extFormat);
    }


}
