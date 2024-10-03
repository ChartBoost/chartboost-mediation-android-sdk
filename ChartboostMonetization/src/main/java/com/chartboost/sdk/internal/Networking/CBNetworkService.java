package com.chartboost.sdk.internal.Networking;

import com.chartboost.sdk.internal.Libraries.TimeSource;
import com.chartboost.sdk.internal.UiPoster;
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer;
import com.chartboost.sdk.internal.logging.Logger;
import com.chartboost.sdk.tracking.EventTracker;

import java.util.concurrent.Executor;

public class CBNetworkService {
    private final Executor networkExecutor;
    private final Executor backgroundExecutor;
    private final NetworkFactory factory;
    private final CBReachability reachability;
    private final TimeSource timeSource;
    private final UiPoster uiPoster;
    private final String appId;
    private final EventTracker eventTracker;

    public CBNetworkService(
            Executor backgroundExecutor,
            NetworkFactory factory,
            CBReachability reachability,
            TimeSource timeSource,
            UiPoster uiPoster,
            Executor networkExecutor,
            EventTracker eventTracker
    ) {
        this.networkExecutor = networkExecutor;
        this.backgroundExecutor = backgroundExecutor;
        this.factory = factory;
        this.reachability = reachability;
        this.timeSource = timeSource;
        this.uiPoster = uiPoster;
        this.appId = ChartboostDependencyContainer.INSTANCE.getAppId();
        this.eventTracker = eventTracker;
    }

    public <T> void submit(CBNetworkRequest<T> request) {
        Logger.v("Execute request: " + request.getUri(), null);
        networkExecutor.execute(
                new NetworkDispatcher<>(
                        backgroundExecutor,
                        factory,
                        reachability,
                        timeSource,
                        uiPoster,
                        request,
                        eventTracker
                )
        );
    }

    public String getAppId() {
        return appId;
    }
}
