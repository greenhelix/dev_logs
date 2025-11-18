package com.innopia.bist.ver2.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.innopia.bist.ver2.data.repository.Test1Repository;
import java.util.List;

public class Test1ViewModel extends ViewModel {

    private static final String TAG = "Test1ViewModel";

    private final Test1Repository test1Model;

    // LiveData - 차트 데이터
    private final MutableLiveData<List<Float>> chartData = new MutableLiveData<>();

    // LiveData - 정보 텍스트
    private final MutableLiveData<String> infoText = new MutableLiveData<>();

    // LiveData - 통계 데이터
    private final MutableLiveData<Test1Repository.DataStats> statsData = new MutableLiveData<>();

    // LiveData - 로딩 상태
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // LiveData - 에러 메시지
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public Test1ViewModel() {
        test1Model = new Test1Repository();
        infoText.setValue("Click button to load data");
    }

    // Getters for LiveData
    public LiveData<List<Float>> getChartData() {
        return chartData;
    }

    public LiveData<String> getInfoText() {
        return infoText;
    }

    public LiveData<Test1Repository.DataStats> getStatsData() {
        return statsData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * 랜덤 데이터 로드
     */
    public void loadRandomData() {
        isLoading.setValue(true);
        infoText.setValue("Loading random data...");

        test1Model.generateRandomData(new Test1Repository.Test1Callback() {
            @Override
            public void onDataGenerated(List<Float> data, String info) {
                Log.d(TAG, "Data generated: " + data.size() + " points");

                chartData.setValue(data);
                infoText.setValue(info);

                // 통계 계산
                Test1Repository.DataStats stats = new Test1Repository.DataStats(data);
                statsData.setValue(stats);

                isLoading.setValue(false);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error: " + error);
                errorMessage.setValue(error);
                infoText.setValue("Error occurred");
                isLoading.setValue(false);
            }
        });
    }

    /**
     * 매출 데이터 로드
     */
    public void loadSalesData() {
        isLoading.setValue(true);
        infoText.setValue("Loading sales data...");

        test1Model.generateSalesData(new Test1Repository.Test1Callback() {
            @Override
            public void onDataGenerated(List<Float> data, String info) {
                Log.d(TAG, "Sales data loaded: " + data.size() + " points");

                chartData.setValue(data);
                infoText.setValue(info);

                // 통계 계산
                Test1Repository.DataStats stats = new Test1Repository.DataStats(data);
                statsData.setValue(stats);

                isLoading.setValue(false);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error: " + error);
                errorMessage.setValue(error);
                infoText.setValue("Error occurred");
                isLoading.setValue(false);
            }
        });
    }
}
