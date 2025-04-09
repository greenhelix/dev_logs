#!/bin/sh

top_dir=`pwd`
ENV_CONF="env.conf"

ENV_Reset()
{
	export path_to_synaptics_sdk=""
	export product=""
	export ref_product=""
	export android_build_config_path=""
	export uboot_config=""

	export INNO_FACTORY=N 
}

set_common_information()
{
	export path_to_top="$top_dir"
	

}