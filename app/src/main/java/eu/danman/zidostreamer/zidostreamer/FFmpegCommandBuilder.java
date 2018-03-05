package eu.danman.zidostreamer.zidostreamer;

/**
 * Created by ferencknebl on 2018. 03. 01..
 */

public class FFmpegCommandBuilder {

    private StringBuilder cmd = new StringBuilder();

    public FFmpegCommandBuilder() {
        this("-i - -codec:v copy -bsf:v dump_extra -f mpegts");
    }

    FFmpegCommandBuilder(String basecmd) {
        setBaseCmd(basecmd);
    }

    public FFmpegCommandBuilder setBaseCmd(String basecmd) {
        cmd.setLength(0);
        cmd.append(basecmd);

        return this;
    }

    public FFmpegCommandBuilder setUrl(String url) {
        if (!url.startsWith("udp://")) {
            url = "udp://" + url;
        }

        cmd.append(url);

        return this;
    }

    public String build() {
        return cmd.toString();
    }
}
