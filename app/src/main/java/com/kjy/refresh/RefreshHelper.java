package com.kjy.refresh;

import android.view.View;

/**
 * Created by eiboran-android001 on 2017/9/15.
 */

public interface RefreshHelper {
    /**
     * 初始化刷新view
     * @return
     */
    View onInitRefreshHeaderView();

    /**
     * 初始化尺寸高度
     * @param originRefreshHeight
     * @return
     */
    boolean onInitRefreshHeight(int originRefreshHeight);

    /**
     * 刷新状态的改变
     * @param refreshView
     * @param refreshState
     */
    void onRefreshStateChanged(View refreshView, int refreshState);
}
