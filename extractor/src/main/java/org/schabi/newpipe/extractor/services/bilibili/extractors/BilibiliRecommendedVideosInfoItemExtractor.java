// File: extractor/src/main/java/org/schabi/newpipe/extractor/services/bilibili/extractors/BilibiliRecommendedVideosInfoItemExtractor.java
package org.schabi.newpipe.extractor.services.bilibili.extractors;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliChannelLinkHandlerFactory;

public class BilibiliRecommendedVideosInfoItemExtractor implements StreamInfoItemExtractor {

    protected final JsonObject item;

    public BilibiliRecommendedVideosInfoItemExtractor(final JsonObject json) {
        item = json;
    }

    @Override
    public String getName() throws ParsingException {
        return item.getString("title");
    }

    @Override
    public String getUrl() throws ParsingException {
        return item.getString("uri") + "?p=1";
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        return item.getString("pic").replace("http:", "https:");
    }

    @Override
    public StreamType getStreamType() throws ParsingException {
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public long getDuration() throws ParsingException {
        return item.getInt("duration");
    }

    @Override
    public long getViewCount() throws ParsingException {
        if (item.has("stat")) {
            return item.getObject("stat").getInt("view");
        }
        return 0;
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        if (item.has("owner")) {
            return BilibiliChannelLinkHandlerFactory.baseUrl + item.getObject("owner").getLong("mid");
        }
        return "";
    }

    @Override
    public String getUploaderName() throws ParsingException {
        if (item.has("owner")) {
            return item.getObject("owner").getString("name");
        }
        return "";
    }

    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        if (item.has("owner")) {
            return item.getObject("owner").getString("face");
        }
        return "";
    }

    @SuppressWarnings("SimpleDateFormat")
    @Override
    public String getTextualUploadDate() throws ParsingException {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(item.getInt("pubdate") * 1000L));
    }

    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return new DateWrapper(LocalDateTime.parse(
                Objects.requireNonNull(getTextualUploadDate()), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atOffset(ZoneOffset.ofHours(+8)));
    }

}