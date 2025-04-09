#!/bin/sh

top_dir=`pwd`

ENV_CONF=".env.conf"

set_common_information()
{
    export path_to_top="$top_dir"
    export path_to_android="$top_dir/android"
    export ANDROID_BUILD_TOP="$path_to_android"
    export BUILD_NUMBER="133"
    export PLATFORM_SECURITY_PATCH="2025-04-01"
    #temp.

    # For Recovery
    export INNO_RECOVERY=N
    # For Sign OTA
    export INNO_OTA_SIGN=Y
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
    export android_build_config_path="vendor/synaptics/${product}/configs/IMTM8300_sl"
    export uboot_config="vs680_a0_uboot_IMTM8300_defconfig"
#For Sign
	export inno_sign_keys="${path_to_synaptics_sdk}/inno_sign_keys"
	export post_processing_path="${path_to_synaptics_sdk}/factory/scripts/vs680/post_processing"
	export release_emmc="${path_to_android}/vendor/synaptics/${product}/build/${product}_sl/release/eMMCimg"
}

IMTM840A_ENV_USERDEBUG()
{
	IMTM840A_ENV
	ENV_USERDEBUG
}

IMTM840A_ENV_USER()
{
	IMTM840A_ENV
	ENV_USER
}

IMTM840A_ENV_ENG()
{
	IMTM840A_ENV
	ENV_ENG
}

IMTM840A_ENV_FACTORY()
{
	export INNO_FACTORY=Y
	IMTM840A_ENV
	ENV_ENG
	set_factory_bluetooth_firmware
}

IMTM8300_ENV_USERDEBUG()
{
	IMTM8300_ENV
	ENV_USERDEBUG
}

IMTM8300_ENV_USER()
{
	IMTM8300_ENV
	ENV_USER
}

IMTM8300_ENV_ENG()
{
	IMTM8300_ENV
	ENV_ENG
}

IMTM8300_ENV_FACTORY()
{
	export INNO_FACTORY=Y
	IMTM8300_ENV
	ENV_ENG
}

set_fw_information()
{
    FW_BUILD_NUMBER=$BUILD_NUMBER # TODO
    # FW_BUILD_DATE=`date "+%Y%m%d"`
    FW_BUILD_DATE="20250402"

    if [ "$product" = "IMTM840A" ]; then
        FW_MODEL_NAME="G8V"
    else
        FW_MODEL_NAME="G8C"
    fi
    export INNO_PROP_SW_VERSION="v12.${FW_MODEL_NAME}.${FW_BUILD_NUMBER}_${FW_BUILD_DATE}"
    export INNO_SW_VERSION="INN_v12.${FW_MODEL_NAME}_DE.${FW_BUILD_NUMBER}_${FW_BUILD_DATE}_${atv_build_type}"

    # For Factory
    if [ "$INNO_FACTORY" = "Y" ]; then
        if [ "$product" = "IMTM840A" ]; then
            export INNO_FACTORY_VERSION=1.10.51
            export INNO_FACTORY_SERVICE_VERSION=INN_v12.G8V_DE.131_20241125_user
            export PLATFORM_SECURITY_PATCH="2024-11-01"
        else # IMTM8300
            export INNO_FACTORY_VERSION=0.99.00
            export INNO_FACTORY_SERVICE_VERSION=INN_v12.G8C_DE.129_20241024_user
        fi
        export INNO_SW_VERSION="${product:3}_FACTORY_V${INNO_FACTORY_VERSION}_${FW_BUILD_DATE}"
        export INNO_PROP_SW_VERSION="${INNO_SW_VERSION:6}"
    fi
}

set_factory_bluetooth_firmware()
{
    # commit f85b5211745bf071dbc1c2fb8b5d7e685f07fa8e
    # Author: polomeria <polomeria@innopia.com>
    # Date:   Tue Nov 19 00:48:43 2024 -0500
    #
    #     Replace the BT firmware for the factory tool
    #     - problem : We can't test bt antenna currently.
    #     - solution : use the other version of bt firmware only for factory tool.
    #
    #     Change-Id: I121e760f0eb8adb2715eb7cb9f9d2e2f0125c9cf
    #
    # A	syna-release-value/linux_5_4/firmware/bcm/BCM4362A2_001.003.006.1091.1172.hcd
    # D	syna-release-value/linux_5_4/firmware/bcm/BCM4362A2_001.003.006.1103.1192.hcd

    if [ "$product" = "IMTM840A" ]; then
        rm -rf ${path_to_synaptics_sdk}/linux_5_4/firmware/bcm/BCM4362A2_001.003.006.1103.1192.hcd
        cp ${path_to_android}/vendor/innopia/factory/bin/bluetooth/BCM4362A2_001.003.006.1091.1172.hcd ${path_to_synaptics_sdk}/linux_5_4/firmware/
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

SELECT_BUILD()
{
	echo ""
	echo "   1) IMTM840A userdebug"
	echo "   2) IMTM840A user"
	echo "   3) IMTM840A eng"
	echo "   4) IMTM8300 userdebug"
	echo "   5) IMTM8300 user"
	echo "   6) IMTM8300 eng"
	echo "   7) IMTM840A factory"
	echo "   8) IMTM8300 factory"
	printf "choice project : "

	read PROJECT_TYPE
        echo "PROJECT_TYPE=$PROJECT_TYPE" > $ENV_CONF
	case $PROJECT_TYPE in
		1 ) IMTM840A_ENV_USERDEBUG;;
		2 ) IMTM840A_ENV_USER;;
		3 ) IMTM840A_ENV_ENG;;
		4 ) IMTM8300_ENV_USERDEBUG;;
		5 ) IMTM8300_ENV_USER;;
		6 ) IMTM8300_ENV_ENG;;
		7 ) IMTM840A_ENV_FACTORY;;
		8 ) IMTM8300_ENV_FACTORY;;
		* ) echo "Invalid project!"; return;;
	esac
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
	1 ) IMTM840A_ENV_USERDEBUG;;
	2 ) IMTM840A_ENV_USER;;
	3 ) IMTM840A_ENV_ENG;;
	4 ) IMTM8300_ENV_USERDEBUG;;
	5 ) IMTM8300_ENV_USER;;
	6 ) IMTM8300_ENV_ENG;;
	7 ) IMTM840A_ENV_FACTORY;;
	8 ) IMTM8300_ENV_FACTORY;;
	* ) echo "Invalid project!"; return;;
esac

set_fw_information

