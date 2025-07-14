#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end

import android.app.Application;
import androidx.annotation.NonNull;
import com.innopia.bist.view.MainViewModel;

public class ${NAME} extends BaseTestViewModel {
    private static final String TAG = "${NAME}";

    public ${NAME}(@NonNull Application application, MainViewModel mainViewModel) {
        super(application, new ${TestModel}(), mainViewModel);
    }

    @Override
    protected String getTag() {
        return TAG;
    }
}
