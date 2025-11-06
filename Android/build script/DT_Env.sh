#!/bin/sh

top_dir=`pwd`

ENV_CONF=".env.conf"

set_common_information()
{
    export path_to_top="$top_dir"
    export path_to_android="$top_dir/android"
    export ANDROID_BUILD_TOP="$path_to_android"
    export PLATFORM_SECURITY_PATCH="2025-09-01"

    # For Factory
    export INNO_FACTORY=N
    # For Recovery
    export INNO_RECOVERY=Y
    # For Sign OTA
    export INNO_OTA_SIGN=Y

    #For Compatiblity with QG2 device based on ATV12
    # ul profile but remvoing GKI,init_boot,verdor_kernle_partion, provision 2.0
    export ul_profile_for_QG2=Y
}

ENV_USERDEBUG()
{
    export atv_build_type=userdebug
}
ENV_USER()
{
    export atv_build_type=user
}
ENV_ENG()
{
    export atv_build_type=eng
}

COPY_CLASSIC_CZSK_RELEASEKEY()
{
    cp $ANDROID_BUILD_TOP/vendor/innopia/security_classic_CZSK/* $ANDROID_BUILD_TOP/build/target/product/security/
}

COPY_CLASSIC_HU_RELEASEKEY()
{
    cp $ANDROID_BUILD_TOP/vendor/innopia/security_classic_HU/* $ANDROID_BUILD_TOP/build/target/product/security/
}

COPY_CLASSIC_RELEASEKEY()
{
	cp $ANDROID_BUILD_TOP/vendor/innopia/security_classic/* $ANDROID_BUILD_TOP/build/target/product/security/
}

COPY_VALUE_RELEASEKEY()
{
	cp $ANDROID_BUILD_TOP/vendor/innopia/security/* $ANDROID_BUILD_TOP/build/target/product/security/
}

CHECKOUT_TESTKEY()
{
	rm $ANDROID_BUILD_TOP/build/make/target/product/security/*
	git checkout $ANDROID_BUILD_TOP/build/make/target/product/security
}

IMTM840A_ENV()
{
	export path_to_synaptics_sdk="$top_dir/syna-release-value"
    export product="IMTM840A"
    export ref_product="IMTM840A"
    export android_build_config_path="vendor/synaptics/${product}/configs/IMTM840A_sl"
    export uboot_config="vs640_a0_uboot_m840a_defconfig"
}

IMTM8300_ENV()
{
    export path_to_synaptics_sdk="$top_dir/syna-release-classic"
    export product="IMTM8300"
    export ref_product="IMTM8300"

    if [ "$ul_profile_for_QG2" = "N" ]; then
	export android_build_config_path="vendor/synaptics/${product}/configs/IMTM8300_ul"
    else
	export android_build_config_path="vendor/synaptics/${product}/configs/IMTM8300_ul_QG2"
    fi

    export uboot_config="vs680_a0_uboot_evk_defconfig"

#For Sign
    export inno_sign_keys="${path_to_synaptics_sdk}/inno_sign_keys"
    export post_processing_path="${path_to_synaptics_sdk}/factory/scripts/vs680/post_processing"
    if [ "$ul_profile_for_QG2" = "N" ]; then
	export release_emmc="${path_to_android}/vendor/synaptics/${product}/build/${product}_ul/release/eMMCimg"
    else
	export release_emmc="${path_to_android}/vendor/synaptics/${product}/build/${product}_ul_QG2/release/eMMCimg"
    fi

    # For Factory
    if [ "$INNO_FACTORY" = "Y" ]; then
        export INNO_FACTORY_VERSION=0.99.00
        export INNO_FACTORY_SERVICE_VERSION=INN_v14.G8C_DE.129_20241024_user
    fi

	# For Wi-Fi country code
	git checkout ${path_to_synaptics_sdk}/linux_5_15/drivers/synaptics/net/wireless/bcmdhd_101.10.591.91.43_ampak/Makefile
}

IMTM8300_DE_ENV()
{
    export path_to_synaptics_sdk="$top_dir/syna-release-classic"
    export product="IMTM8300_DE"
    export ref_product="IMTM8300_DE"
	IMTM8300_XX_ENV
}

IMTM8300_CZSK_ENV()
{
    export path_to_synaptics_sdk="$top_dir/syna-release-classic"
    export product="IMTM8300_CZSK"
    export ref_product="IMTM8300_CZSK"
	IMTM8300_XX_ENV
}

IMTM8300_HU_ENV()
{
    export path_to_synaptics_sdk="$top_dir/syna-release-classic"
    export product="IMTM8300_HU"
    export ref_product="IMTM8300_HU"
	IMTM8300_XX_ENV
}

IMTM8300_XX_ENV()
{
    if [ "$INNO_FACTORY" = "Y" ]; then
	    export android_build_config_path="vendor/synaptics/${product}/configs/${product}_ul_factory"
	    export release_emmc="${path_to_android}/vendor/synaptics/${product}/build/${product}_ul_factory/release/eMMCimg"
    else
	    export android_build_config_path="vendor/synaptics/${product}/configs/${product}_ul"
	    export release_emmc="${path_to_android}/vendor/synaptics/${product}/build/${product}_ul/release/eMMCimg"
    fi

    export uboot_config="vs680_a0_uboot_evk_defconfig"

#IMTM8300_DE must be forcibly set to "N" for ul_profile_for_QG2.
    export ul_profile_for_QG2=N
#For Sign
    export inno_sign_keys="${path_to_synaptics_sdk}/inno_sign_keys"
    export post_processing_path="${path_to_synaptics_sdk}/factory/scripts/vs680/post_processing"

    # For Factory
    if [ "$INNO_FACTORY" = "Y" ]; then
        export INNO_FACTORY_VERSION=0.99.02
        if [ "$INNO_FACTORY_SERVICE_VERSION" = "" ] || [ "$INNO_FACTORY_SERVICE_VERSION" = "UNKNOWN" ]; then
            echo ""
            echo "WARNING ========================================="
            echo "WARNING INNO_FACTORY_SERVICE_VERSION is NOT valid"
            echo "WARNING ========================================="
            echo ""
            return 1
        fi
    fi

	# For Wi-Fi country code
    if [ "$INNO_FACTORY" != "Y" ]; then
        if [ "$product" = "IMTM8300_CZSK" ] || [ "$product" = "IMTM8300_HU" ]; then
            sed -i 's/^#CONFIG_BCMDHD_NO_POWER_OFF/CONFIG_BCMDHD_NO_POWER_OFF/' ${path_to_synaptics_sdk}/linux_5_15/drivers/synaptics/net/wireless/bcmdhd_101.10.591.91.43_ampak/Makefile
        else
            git checkout ${path_to_synaptics_sdk}/linux_5_15/drivers/synaptics/net/wireless/bcmdhd_101.10.591.91.43_ampak/Makefile
        fi
    fi
}

REMOVE_IMTM8300_VENDOR_FOLDER()
{
    echo "remove ${path_to_android}/vendor/synaptics/IMTM8300*"
	rm -rf ${path_to_android}/vendor/synaptics/IMTM8300*
    echo "git checkout ${path_to_android}/vendor/synaptics/IMTM8300*"
	git checkout ${path_to_android}/vendor/synaptics/IMTM8300*
}

REMOVE_IMTM840A_VENDOR_FOLDER()
{
    echo "remove ${path_to_android}/vendor/synaptics/IMTM840A*"
	rm -rf ${path_to_android}/vendor/synaptics/IMTM840A*
    echo "git checkout ${path_to_android}/vendor/synaptics/IMTM840A*"
	git checkout ${path_to_android}/vendor/synaptics/IMTM840A*
}

set_fw_information()
{
    if [ "$product" = "IMTM8300_CZSK" ]; then
	#CZSK FW version code
	export BUILD_NUMBER="301.1"
    
	elif [ "$product" = "IMTM8300_HU" ]; then
	#HU FW version code
	export BUILD_NUMBER="401.5N_sepolicy"

    elif [ "$product" = "IMTM8300_DE" ]; then
	#IMTM8400_DE FW version code
	export BUILD_NUMBER="205"
    
	else
	#IMTM840A FW Version code 
	export BUILD_NUMBER="152.5_matrix"
    fi

    FW_BUILD_NUMBER=$BUILD_NUMBER # TODO
    FW_BUILD_DATE=`date "+%Y%m%d"`
    # FW_BUILD_DATE="20250912"

    if [ "$product" = "IMTM840A" ]; then
        FW_MODEL_NAME="G8V"
    else
        FW_MODEL_NAME="G8C"
    fi
    export INNO_PROP_SW_VERSION="v14.${FW_MODEL_NAME}.${FW_BUILD_NUMBER}_${FW_BUILD_DATE}"

    if [ "$product" = "IMTM8300_CZSK" ]; then
	export INNO_SW_VERSION="INN_v14.${FW_MODEL_NAME}_CZSK.${FW_BUILD_NUMBER}_${FW_BUILD_DATE}_${atv_build_type}"
    elif [ "$product" = "IMTM8300_HU" ]; then
	export INNO_SW_VERSION="INN_v14.${FW_MODEL_NAME}_HU.${FW_BUILD_NUMBER}_${FW_BUILD_DATE}_${atv_build_type}"
    else
	export INNO_SW_VERSION="INN_v14.${FW_MODEL_NAME}_DE.${FW_BUILD_NUMBER}_${FW_BUILD_DATE}_${atv_build_type}"
    fi

    # For Factory
    if [ "$INNO_FACTORY" = "Y" ]; then
        if [ "$product" = "IMTM840A" ]; then
            export INNO_FACTORY_VERSION=1.10.51
            export INNO_FACTORY_SERVICE_VERSION=INN_v12.G8V_DE.131_20241125_user
            export PLATFORM_SECURITY_PATCH="2024-11-01"
		fi
        export INNO_SW_VERSION="${product:3}_FACTORY_V${INNO_FACTORY_VERSION}_${FW_BUILD_DATE}"
        export INNO_PROP_SW_VERSION="${INNO_SW_VERSION:6}"
    fi
}

set_factory_button_key()
{
    #In factory mode, the Button key is not used as a global key.
    if [ "$product" = "IMTM840A" ]; then
        cp ${path_to_android}/vendor/innopia/factory/config/global_keys_factory.xml ${path_to_android}/device/synaptics/common/overlay/atv/TvSynaFrameworkOverlay/res/xml/global_keys.xml
    fi
}

ENV_Reset ()
{
    export path_to_synaptics_sdk=""
    export product=""
    export ref_product=""
    export android_build_config_path=""
    export uboot_config=""

    # For Factory
    export INNO_FACTORY=N
}
ENV_Help ()
{
        clear
        echo ""
        echo "##########################################################################################"
        echo "#"
        echo "#  . ./Env.sh [help|reset]"
        echo "#"
        echo "##########################################################################################"
        echo ""
}

REMOVE_VENDOR_FOLDER()
{
    if [[ "$product" == *"IMTM8300"* ]] ; then
        REMOVE_IMTM8300_VENDOR_FOLDER
        REMOVE_IMTM840A_VENDOR_FOLDER
    elif [[ "$product" == *"IMTM840A"* ]] ; then
        REMOVE_IMTM8300_VENDOR_FOLDER
    fi
}

DOWNLOAD_TTS_FILE()
{
    echo "download tts files..."
    #rm -rf voice_data
    rm -rf android/vendor/innopia/tts
    mkdir android/vendor/innopia/tts
    python3 voice_downloader.py --apk=android/vendor/google_gtvs/apps/arm64-v8a/GoogleTTS.apk --output_dir=voice_data --locales="de-de cs-cz hr-hr sk-sk hu-hu sq-al el-gr pl-pl sr-rs"
    cp -rf voice_data/* android/vendor/innopia/tts
    echo "done"
}

SELECT_BUILD()
{
	echo ""
	echo "   1) IMTM8300 userdebug"
	echo "   2) IMTM8300 user"
	echo "   3) IMTM8300 eng"
	echo "   4) IMTM8300_DE userdebug"
	echo "   5) IMTM8300_DE user"
	echo "   6) IMTM8300_DE eng"
	echo "   7) IMTM8300_CZSK userdebug"
	echo "   8) IMTM8300_CZSK user"
	echo "   9) IMTM8300_CZSK eng"
	echo "   10) IMTM8300_HU userdebug"
	echo "   11) IMTM8300_HU user"
	echo "   12) IMTM8300_HU eng"
	echo "   21) IMTM840A userdebug"
	echo "   22) IMTM840A user"
	echo "   23) IMTM840A eng"
	echo "   24) IMTM840A factory"
	echo "   31) IMTM8300_DE factory"
	echo "   32) IMTM8300_CZSK factory"
	echo "   33) IMTM8300_HU factory"
	printf "choice project : "

	read PROJECT_TYPE
		echo "PROJECT_TYPE=$PROJECT_TYPE" > $ENV_CONF
	case $PROJECT_TYPE in
		1 ) IMTM8300_ENV
			ENV_USERDEBUG
			CHECKOUT_TESTKEY
		;;
		2 ) IMTM8300_ENV
			ENV_USER
			COPY_CLASSIC_RELEASEKEY
		;;
		3 ) IMTM8300_ENV
			ENV_ENG
			COPY_CLASSIC_RELEASEKEY
		;;
		4 ) IMTM8300_DE_ENV
			ENV_USERDEBUG
			CHECKOUT_TESTKEY
		;;
		5 ) IMTM8300_DE_ENV
			ENV_USER
			COPY_CLASSIC_RELEASEKEY
		;;
		6 ) IMTM8300_DE_ENV
			ENV_ENG
			COPY_CLASSIC_RELEASEKEY
		;;
		7 ) IMTM8300_CZSK_ENV
			ENV_USERDEBUG
			CHECKOUT_TESTKEY
		;;
		8 ) IMTM8300_CZSK_ENV
			ENV_USER
			COPY_CLASSIC_CZSK_RELEASEKEY
		;;
		9 ) IMTM8300_CZSK_ENV
			ENV_ENG
			COPY_CLASSIC_CZSK_RELEASEKEY
		;;
		10 ) IMTM8300_HU_ENV
			ENV_USERDEBUG
			CHECKOUT_TESTKEY
		;;
		11 ) IMTM8300_HU_ENV
			ENV_USER
			COPY_CLASSIC_HU_RELEASEKEY
		;;
		12 ) IMTM8300_HU_ENV
			ENV_ENG
			COPY_CLASSIC_HU_RELEASEKEY
		;;
		21 ) IMTM840A_ENV
			ENV_USERDEBUG
			CHECKOUT_TESTKEY
		;;
		22 ) IMTM840A_ENV
			ENV_USER
			COPY_VALUE_RELEASEKEY
		;;
		23 ) IMTM840A_ENV
			ENV_ENG
			COPY_VALUE_RELEASEKEY
		;;
		24 ) IMTM840A_ENV
			export INNO_FACTORY=Y
			ENV_ENG
			COPY_VALUE_RELEASEKEY
			set_factory_button_key
		;;
		31 ) export INNO_FACTORY=Y
			 export INNO_RECOVERY=N
			 export INNO_OTA_SIGN=N
			 export INNO_FACTORY_SERVICE_VERSION=INN_v14.G8C_DE.205_20250804_user
			 IMTM8300_DE_ENV
			 ENV_USERDEBUG
			 CHECKOUT_TESTKEY
		;;
		32 ) export INNO_FACTORY=Y
			 export INNO_RECOVERY=N
			 export INNO_OTA_SIGN=N
			 export INNO_FACTORY_SERVICE_VERSION=INN_v14.G8C_CZSK.302_20250820_user
			 IMTM8300_CZSK_ENV
			 ENV_USERDEBUG
			 CHECKOUT_TESTKEY
		;;
		33 ) export INNO_FACTORY=Y
			 export INNO_RECOVERY=N
			 export INNO_OTA_SIGN=N
             export INNO_FACTORY_SERVICE_VERSION=UNKNOWN # TODO
			 IMTM8300_HU_ENV
			 ENV_USERDEBUG
			 CHECKOUT_TESTKEY
		;;
		* ) echo "Invalid project!"; return;;
	esac

    
    REMOVE_VENDOR_FOLDER
}

ENV_Reset

set_common_information

# Argument check
if [ $# -eq 0 ]; then
        if [ -f "$ENV_CONF" ]; then
                . ./$ENV_CONF
    else
        SELECT_BUILD
        fi
else
        ENV_Help
        return
fi
case $PROJECT_TYPE in
	1 ) IMTM8300_ENV
		ENV_USERDEBUG
        CHECKOUT_TESTKEY
		;;
	2 ) IMTM8300_ENV
		ENV_USER
        COPY_CLASSIC_RELEASEKEY
		;;
	3 ) IMTM8300_ENV
		ENV_ENG
        COPY_CLASSIC_RELEASEKEY
		;;
	4 ) IMTM8300_DE_ENV
		ENV_USERDEBUG
        CHECKOUT_TESTKEY
		;;
	5 ) IMTM8300_DE_ENV
		ENV_USER
        COPY_CLASSIC_RELEASEKEY
		;;
	6 ) IMTM8300_DE_ENV
		ENV_ENG
        COPY_CLASSIC_RELEASEKEY
		;;
	7 ) IMTM8300_CZSK_ENV
		ENV_USERDEBUG
        CHECKOUT_TESTKEY
		;;
	8 ) IMTM8300_CZSK_ENV
		ENV_USER
        COPY_CLASSIC_CZSK_RELEASEKEY
		;;
	9 ) IMTM8300_CZSK_ENV
		ENV_ENG
        COPY_CLASSIC_CZSK_RELEASEKEY
		;;
	10 ) IMTM8300_HU_ENV
		ENV_USERDEBUG
        CHECKOUT_TESTKEY
		;;
	11 ) IMTM8300_HU_ENV
		ENV_USER
        COPY_CLASSIC_HU_RELEASEKEY
		;;
	12 ) IMTM8300_HU_ENV
		ENV_ENG
        COPY_CLASSIC_HU_RELEASEKEY
		;;
	21 ) IMTM840A_ENV
		ENV_USERDEBUG
        CHECKOUT_TESTKEY
		;;
	22 ) IMTM840A_ENV
		ENV_USER
        COPY_VALUE_RELEASEKEY
		;;
	23 ) IMTM840A_ENV
		ENV_ENG
        COPY_VALUE_RELEASEKEY
		;;
	24 ) IMTM840A_ENV
		export INNO_FACTORY=Y
		ENV_ENG
        COPY_VALUE_RELEASEKEY
		set_factory_button_key
		;;
	31 ) export INNO_FACTORY=Y
		 export INNO_RECOVERY=N
		 export INNO_OTA_SIGN=N
		 export INNO_FACTORY_SERVICE_VERSION=INN_v14.G8C_DE.205_20250804_user
         export PLATFORM_SECURITY_PATCH="2025-01-01"
		 IMTM8300_DE_ENV
		 ENV_USERDEBUG
		 CHECKOUT_TESTKEY
		 ;;
	32 ) export INNO_FACTORY=Y
		 export INNO_RECOVERY=N
		 export INNO_OTA_SIGN=N
		 export INNO_FACTORY_SERVICE_VERSION=INN_v14.G8C_CZSK.302_20250820_user
         export PLATFORM_SECURITY_PATCH="2025-01-01"
		 IMTM8300_CZSK_ENV
		 ENV_USERDEBUG
		 CHECKOUT_TESTKEY
		 ;;
	33 ) export INNO_FACTORY=Y
		 export INNO_RECOVERY=N
		 export INNO_OTA_SIGN=N
		 export INNO_FACTORY_SERVICE_VERSION=UNKNOWN # TODO
         export PLATFORM_SECURITY_PATCH="2025-01-01"
		 IMTM8300_HU_ENV
		 ENV_USERDEBUG
		 CHECKOUT_TESTKEY
		 ;;
	* ) echo "Invalid project!"; return;;
esac

DOWNLOAD_TTS_FILE
set_fw_information

