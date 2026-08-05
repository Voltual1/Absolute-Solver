// File: DanmakuFlameMaster/src/main/java/master/flame/danmaku/danmaku/parser/BiliDanmukuParser.java
package master.flame.danmaku.danmaku.parser;

import android.graphics.Color;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItem;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItem.Position;
import java.util.List;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

import static master.flame.danmaku.danmaku.model.IDanmakus.ST_BY_TIME;

public class BiliDanmukuParser extends BaseDanmakuParser {

    private List<BulletCommentsInfoItem> mCommentsList;

    public BiliDanmukuParser(List<BulletCommentsInfoItem> commentsList) {
        this.mCommentsList = commentsList;
    }

    @Override
    public Danmakus parse() {
        Danmakus danmakus = new Danmakus(ST_BY_TIME, false, mContext.getBaseComparator());
        if (mCommentsList == null || mCommentsList.isEmpty()) {
            return danmakus;
        }

        int index = 0;
        Object lock = danmakus.obtainSynchronizer();
        synchronized (lock) {
            for (BulletCommentsInfoItem item : mCommentsList) {
                if (item == null) continue;

                // 映射弹幕位置与类型
                int danmakuType = BaseDanmaku.TYPE_SCROLL_RL;
                if (item.getPosition() == Position.BOTTOM) {
                    danmakuType = BaseDanmaku.TYPE_FIX_BOTTOM;
                } else if (item.getPosition() == Position.TOP) {
                    danmakuType = BaseDanmaku.TYPE_FIX_TOP;
                }

                BaseDanmaku danmaku = mContext.mDanmakuFactory.createDanmaku(danmakuType, mContext);
                if (danmaku != null) {
                    // 时间校正：Extractor 解析时加入了 +2500ms 进行对齐，这里减去以恢复为最真实的弹幕时间
                    long rawTime = item.getDuration().toMillis() - 2500;
                    danmaku.setTime(Math.max(0L, rawTime));
                    
                    // 属性映射
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
}