package com.kjy;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.kjy.refresh.RefreshHelper;
import com.kjy.refresh.RefreshView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RefreshView rv_refresh;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rv_refresh = (RefreshView) findViewById(R.id.rv_refresh);
        rv_refresh.setRefreshHelper(new RefreshHelper() {
            //初始化刷新view
            @Override
            public View onInitRefreshHeaderView() {
                return LayoutInflater.from(MainActivity.this).inflate(R.layout.refresh_head, null);
            }

            //初始化尺寸高度
            @Override
            public boolean onInitRefreshHeight(int originRefreshHeight) {
                rv_refresh.setRefreshNormalHeight(0);
                rv_refresh.setRefreshingHeight(rv_refresh.getOriginRefreshHeight());
                rv_refresh.setRefreshArrivedStateHeight(rv_refresh.getOriginRefreshHeight());
                return false;
            }

            //刷新状态的改变
            @Override
            public void onRefreshStateChanged(View refreshView, int refreshState) {
                TextView tv = (TextView) refreshView.findViewById(R.id.tv_head);
                switch (refreshState) {
                    case RefreshView.STATE_REFRESH_NORMAL:
                        tv.setText("正常状态");
                        break;
                    case RefreshView.STATE_REFRESH_NOT_ARRIVED:
                        tv.setText("下拉刷新");
                        break;
                    case RefreshView.STATE_REFRESH_ARRIVED:
                        tv.setText("松开刷新");
                        break;
                    case RefreshView.STATE_REFRESHING:
                        tv.setText("正在刷新");
                        new Thread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Thread.sleep(1000l);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    rv_refresh.onCompleteRefresh();
                                                }
                                            });
                                        } catch (InterruptedException e) {
                                        }
                                    }
                                }
                        ).start();
                        break;
                }
            }
        });
    }
}
