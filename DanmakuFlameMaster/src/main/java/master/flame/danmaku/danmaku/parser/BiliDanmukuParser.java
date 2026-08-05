// File: DanmakuFlameMaster/src/main/java/master/flame/danmaku/danmaku/parser/BiliDanmukuParser.java
package master.flame.danmaku.danmaku.parser;

import android.graphics.Color;
import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import java.io.IOException;
import java.util.Locale;
import java.util.List;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItem;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItem.Position;
import master.flame.danmaku.danmaku.model.AlphaValue;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.Duration;
import master.flame.danmaku.danmaku.model.SpecialDanmaku;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.DanmakuFactory;
import master.flame.danmaku.danmaku.parser.android.AndroidFileSource;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

import static master.flame.danmaku.danmaku.model.IDanmakus.ST_BY_TIME;

public class BiliDanmukuParser extends BaseDanmakuParser {

    static {
        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
    }

    private List<BulletCommentsInfoItem> mCommentsList = null;

    // 默认无参构造函数：用于本地离线弹幕 XML 文件解析
    public BiliDanmukuParser() {
    }

    // 带参构造函数：用于在线播放 Extractor 内存弹幕解析
    public BiliDanmukuParser(List<BulletCommentsInfoItem> commentsList) {
        this.mCommentsList = commentsList;
    }

    @Override
    public Danmakus parse() {
        // 模式 1：在线 Extractor 弹幕列表解析
        if (mCommentsList != null) {
            Danmakus danmakus = new Danmakus(ST_BY_TIME, false, mContext.getBaseComparator());
            int index = 0;
            Object lock = danmakus.obtainSynchronizer();
            synchronized (lock) {
                for (BulletCommentsInfoItem item : mCommentsList) {
                    if (item == null) continue;

                    int danmakuType = BaseDanmaku.TYPE_SCROLL_RL;
                    if (item.getPosition() == Position.BOTTOM) {
                        danmakuType = BaseDanmaku.TYPE_FIX_BOTTOM;
                    } else if (item.getPosition() == Position.TOP) {
                        danmakuType = BaseDanmaku.TYPE_FIX_TOP;
                    }

                    BaseDanmaku danmaku = mContext.mDanmakuFactory.createDanmaku(danmakuType, mContext);
                    if (danmaku != null) {
                        long rawTime = item.getDuration().toMillis() - 2500;
                        danmaku.setTime(Math.max(0L, rawTime));
                        danmaku.textSize = (float) (item.getRelativeFontSize() * 41.6f * (mDispDensity - 0.6f));
                        danmaku.textColor = item.getArgbColor();
                        
                        float[] hsv = new float[3];
                        Color.colorToHSV(danmaku.textColor, hsv);
                        danmaku.textShadowColor = hsv[2] < 0.1f ? Color.WHITE : Color.BLACK;

                        DanmakuUtils.fillText(danmaku, item.getCommentText());
                        danmaku.index = index++;
                        danmaku.flags = mContext.mGlobalFlagValues;
                        danmaku.setTimer(mTimer);
                        
                        danmakus.addItem(danmaku);
                    }
                }
            }
            return danmakus;
        }

        // 模式 2：本地离线 XML 文件 SAX 解析
        if (mDataSource != null) {
            AndroidFileSource source = (AndroidFileSource) mDataSource;
            try {
                XMLReader xmlReader = XMLReaderFactory.createXMLReader();
                XmlContentHandler contentHandler = new XmlContentHandler();
                xmlReader.setContentHandler(contentHandler);
                xmlReader.parse(new InputSource(source.data()));
                return contentHandler.getResult();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public class XmlContentHandler extends DefaultHandler {

        private static final String TRUE_STRING = "true";

        public Danmakus result;

        public BaseDanmaku item = null;

        public boolean completed = false;

        public int index = 0;

        public Danmakus getResult() {
            return result;
        }

        @Override
        public void startDocument() throws SAXException {
            result = new Danmakus(ST_BY_TIME, false, mContext.getBaseComparator());
        }

        @Override
        public void endDocument() throws SAXException {
            completed = true;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            String tagName = localName.length() != 0 ? localName : qName;
            tagName = tagName.toLowerCase(Locale.getDefault()).trim();
            if (tagName.equals("d")) {
                String pValue = attributes.getValue("p");
                String[] values = pValue.split(",");
                if (values.length > 0) {
                    long time = (long) (parseFloat(values[0]) * 1000);
                    int type = parseInteger(values[1]);
                    float textSize = parseFloat(values[2]);
                    int color = (int) ((0x00000000ff000000 | parseLong(values[3])) & 0x00000000ffffffff);
                    item = mContext.mDanmakuFactory.createDanmaku(type, mContext);
                    if (item != null) {
                        item.setTime(time);
                        item.textSize = textSize * (mDispDensity - 0.6f);
                        item.textColor = color;
                        float[] hsv = new float[3];
                        Color.colorToHSV(color, hsv);
                        item.textShadowColor = hsv[2] < 0.1 ? Color.WHITE : Color.BLACK;
                    }
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (item != null && item.text != null) {
                if (item.duration != null) {
                    String tagName = localName.length() != 0 ? localName : qName;
                    if (tagName.equalsIgnoreCase("d")) {
                        item.setTimer(mTimer);
                        item.flags = mContext.mGlobalFlagValues;
                        Object lock = result.obtainSynchronizer();
                        synchronized (lock) {
                            result.addItem(item);
                        }
                    }
                }
                item = null;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (item != null) {
                DanmakuUtils.fillText(item, decodeXmlString(new String(ch, start, length)));
                item.index = index++;

                String text = String.valueOf(item.text).trim();
                if (item.getType() == BaseDanmaku.TYPE_SPECIAL && text.startsWith("[")
                        && text.endsWith("]")) {
                    String[] textArr = null;
                    try {
                        JSONArray jsonArray = new JSONArray(text);
                        textArr = new String[jsonArray.length()];
                        for (int i = 0; i < textArr.length; i++) {
                            textArr[i] = jsonArray.getString(i);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (textArr == null || textArr.length < 5 || TextUtils.isEmpty(textArr[4])) {
                        item = null;
                        return;
                    }
                    DanmakuUtils.fillText(item, textArr[4]);
                    float beginX = parseFloat(textArr[0]);
                    float beginY = parseFloat(textArr[1]);
                    float endX = beginX;
                    float endY = beginY;
                    String[] alphaArr = textArr[2].split("-");
                    int beginAlpha = (int) (AlphaValue.MAX * parseFloat(alphaArr[0]));
                    int endAlpha = beginAlpha;
                    if (alphaArr.length > 1) {
                        endAlpha = (int) (AlphaValue.MAX * parseFloat(alphaArr[1]));
                    }
                    long alphaDuraion = (long) (parseFloat(textArr[3]) * 1000);
                    long translationDuration = alphaDuraion;
                    long translationStartDelay = 0;
                    float rotateY = 0, rotateZ = 0;
                    if (textArr.length >= 7) {
                        rotateZ = parseFloat(textArr[5]);
                        rotateY = parseFloat(textArr[6]);
                    }
                    if (textArr.length >= 11) {
                        endX = parseFloat(textArr[7]);
                        endY = parseFloat(textArr[8]);
                        if (!"".equals(textArr[9])) {
                            translationDuration = parseInteger(textArr[9]);
                        }
                        if (!"".equals(textArr[10])) {
                            translationStartDelay = (long) (parseFloat(textArr[10]));
                        }
                    }
                    if (isPercentageNumber(textArr[0])) {
                        beginX *= DanmakuFactory.BILI_PLAYER_WIDTH;
                    }
                    if (isPercentageNumber(textArr[1])) {
                        beginY *= DanmakuFactory.BILI_PLAYER_HEIGHT;
                    }
                    if (textArr.length >= 8 && isPercentageNumber(textArr[7])) {
                        endX *= DanmakuFactory.BILI_PLAYER_WIDTH;
                    }
                    if (textArr.length >= 9 && isPercentageNumber(textArr[8])) {
                        endY *= DanmakuFactory.BILI_PLAYER_HEIGHT;
                    }
                    item.duration = new Duration(alphaDuraion);
                    item.rotationZ = rotateZ;
                    item.rotationY = rotateY;
                    mContext.mDanmakuFactory.fillTranslationData(item, beginX,
                            beginY, endX, endY, translationDuration, translationStartDelay);
                    mContext.mDanmakuFactory.fillAlphaData(item, beginAlpha, endAlpha, alphaDuraion);

                    if (textArr.length >= 12) {
                        if (!TextUtils.isEmpty(textArr[11]) && TRUE_STRING.equalsIgnoreCase(textArr[11])) {
                            item.textShadowColor = Color.TRANSPARENT;
                        }
                    }
                    if (textArr.length >= 14) {
                        ((SpecialDanmaku) item).isQuadraticEaseOut = ("0".equals(textArr[13]));
                    }
                    if (textArr.length >= 15) {
                        if (!"".equals(textArr[14])) {
                            String motionPathString = textArr[14].substring(1);
                            if (!TextUtils.isEmpty(motionPathString)) {
                                String[] pointStrArray = motionPathString.split("L");
                                if (pointStrArray.length > 0) {
                                    float[][] points = new float[pointStrArray.length][2];
                                    for (int i = 0; i < pointStrArray.length; i++) {
                                        String[] pointArray = pointStrArray[i].split(",");
                                        if (pointArray.length >= 2) {
                                            points[i][0] = parseFloat(pointArray[0]);
                                            points[i][1] = parseFloat(pointArray[1]);
                                        }
                                    }
                                    mContext.mDanmakuFactory.fillLinePathData(item, points);
                                }
                            }
                        }
                    }
                }
            }
        }

        private String decodeXmlString(String title) {
            if (title.contains("&amp;")) {
                title = title.replace("&amp;", "&");
            }
            if (title.contains("&quot;")) {
                title = title.replace("&quot;", "\"");
            }
            if (title.contains("&gt;")) {
                title = title.replace("&gt;", ">");
            }
            if (title.contains("&lt;")) {
                title = title.replace("&lt;", "<");
            }
            return title;
        }
    }

    private boolean isPercentageNumber(String number) {
        return number != null && number.contains(".");
    }

    private float parseFloat(String floatStr) {
        try {
            return Float.parseFloat(floatStr);
        } catch (NumberFormatException e) {
            return 0.0f;
        }
    }

    private int parseInteger(String intStr) {
        try {
            return Integer.parseInt(intStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long parseLong(String longStr) {
        try {
            return Long.parseLong(longStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}