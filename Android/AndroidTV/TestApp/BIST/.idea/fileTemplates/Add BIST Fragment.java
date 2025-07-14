#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end

import androidx.fragment.app.Fragment;

public class ${NAME} extends Fragment {

    public static ${NAME} newInstance() {
        return new ${NAME}();
    }
}