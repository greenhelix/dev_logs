package com.innopia.factorytools.tcp;

public class DefProtocol {
	// FLAG
	public static final int DB_REQUEST		= 0xDB01;
	public static final int DB_REQUEST_DISCONNECT	= 0xDB02;
	public static final int DB_REQ_MODEL		= 0xDB03;
	public static final int DB_WILL_SEND_KEY	= 0xDB04;
	public static final int DB_UPDATE_COMPLETE_1ST	= 0xDB05;
	public static final int DB_UPDATE_COMPLETE_2ND	= 0xDB06;
	public static final int DB_REQ_KEY_DATA		= 0xDB07;
	public static final int DB_PING			= 0xDB08;

	public static final int DB_UPDATE_FAILED_1ST	= 0xDB09;
	public static final int DB_UPDATE_FAILED_2ND	= 0xDB0A;

	public static final int DB_SECURE_REQUEST	= 0xDB0B;
	public static final int DB_REQ_DATA_CHECK	= 0xDB0C;

	public static final int DB_UPDATE_FAILED_3RD	= 0xDB0D;
	public static final int DB_REQ_CHECK_1 		= 0xDB0E ;


	public static final int DB_REQ_MODEL_C1 	= 0xDB11 ;
	public static final int DB_REQ_MODEL_C2 	= 0xDB12 ;
	public static final int DB_REQ_MODEL_C3 	= 0xDB13 ;
	public static final int DB_REQ_MODEL_C4 	= 0xDB14 ;
	public static final int DB_REQ_MODEL_C5 	= 0xDB15 ;

	// for Pretest ex) M6760 Audio Test
	public static final int DB_REQ_PRETEST_1 	= 0xDB31 ;
	public static final int DB_REQ_PRETEST_2 	= 0xDB32 ;
	public static final int DB_REQ_PRETEST_3 	= 0xDB33 ;

	// for Request DataP ex) M6760 FCT-DATA
	public static final int DB_REQ_DATA_1 		= 0xDB41 ;
	public static final int DB_REQ_DATA_2 		= 0xDB42 ;
	public static final int DB_REQ_DATA_3 		= 0xDB43 ;
	public static final int DB_REQ_DATA_4 		= 0xDB44 ;

	// FLAG - New Type
	public static final int DB_UPDATE_COMPLETE_C1 	= 0xDB51 ;
	public static final int DB_UPDATE_COMPLETE_C2 	= 0xDB52 ;
	public static final int DB_UPDATE_COMPLETE_C3 	= 0xDB53 ;
	public static final int DB_UPDATE_COMPLETE_C4 	= 0xDB54 ;
	public static final int DB_UPDATE_COMPLETE_C5 	= 0xDB55 ;

	public static final int DB_STEP_1		= 0xDB61 ;
	public static final int DB_STEP_2		= 0xDB62 ;
	public static final int DB_STEP_3		= 0xDB63 ;
	public static final int DB_STEP_4		= 0xDB64 ;
	public static final int DB_STEP_5		= 0xDB65 ;
	public static final int DB_STEP_6		= 0xDB66 ;
	public static final int DB_STEP_7		= 0xDB67 ;
	public static final int DB_STEP_8		= 0xDB68 ;

	///////////

	public static final int FA_ACK			= 0xFA01;
	public static final int FA_ACK_COMPLETE		= 0xFA02;
	public static final int FA_PING			= 0xFA03;
	public static final int FA_SECURE_ACK		= 0xFA04;
	public static final int FA_ACK_CHECK_1 		= 0xFA05 ;
	public static final int FA_SECURE_ACK_SKIP 	= 0xFA06 ;
	public static final int FA_READY_C1 		= 0xFA07 ;
	public static final int FA_READY_C2 		= 0xFA08 ;
	public static final int FA_READY_C3 		= 0xFA09 ;
	public static final int FA_READY_C4 		= 0xFA0A ;
	public static final int FA_READY_C5 		= 0xFA0B ;


	public static final int FA_ACK_COMPLETE_C1 	= 0xFA11 ;
	public static final int FA_ACK_COMPLETE_C2 	= 0xFA12 ;
	public static final int FA_ACK_COMPLETE_C3 	= 0xFA13 ;
	public static final int FA_ACK_COMPLETE_C4 	= 0xFA14 ;
	public static final int FA_ACK_COMPLETE_C5 	= 0xFA15 ;


	// for Pretest ex) M6760 Audio Test
	public static final int FA_REQ_PRETEST_1 	= 0xFA31 ;
	public static final int FA_REQ_PRETEST_2 	= 0xFA32 ;
	public static final int FA_REQ_PRETEST_3 	= 0xFA33 ;
	public static final int FA_REQ_PRETEST_4 	= 0xFA34 ;


	public static final int FA_REQ_AUDIO_11 	= 0xFA41 ;
	public static final int FA_REQ_AUDIO_12 	= 0xFA42 ;
	public static final int FA_REQ_AUDIO_21 	= 0xFA43 ;
	public static final int FA_REQ_AUDIO_22 	= 0xFA44 ;

	public static final int FA_REQ_STEP 		= 0xFA51 ;

	// Tag - common
	public static final int TAG_DATA_HEADER		= 0x1220B1A0;
	public static final int TAG_PKI_DATA_HEADER	= 0x1220B1A1;
	public static final int TAG_AES_DATA_HEADER	= 0x1220B1A2;

	// Tag - Tail Command
	public static final int TAG_TAIL_MODEL		= 0x1C3D0101;
	public static final int TAG_TAIL_REQ_KEY_DATA	= 0x1C3D0102;
	public static final int TAG_TAIL_ACK_COMPLETE	= 0x1C3D0103;
	public static final int TAG_TAIL_MODEL_2ND	= 0x1C3D0104;
	public static final int TAG_TAIL_PCB_SN_C1	= 0x1C3D0105 ;
	public static final int TAG_TAIL_MODEL_C1	= 0x1C3D0106 ;

	public static final int TAG_TAIL_KEY_1		= 0x1C3D0201;
	public static final int TAG_TAIL_CLIENT_2ND	= TAG_TAIL_KEY_1;
	public static final int TAG_TAIL_SEND_DATA_2ND	= 0x1C3D0202;
	public static final int TAG_TAIL_CAL_1		= 0x1C3D0301;
	public static final int TAG_TAIL_CLIENT_1ST	= TAG_TAIL_CAL_1;
	public static final int TAG_TAIL_ERROR		= 0x1C3D0401;
	public static final int TAG_TAIL_DONE		= 0x1C3D0402; // ADD for DT Project
	public static final int TAG_TAIL_CLIENT_3RD	= 0x1C3D0501;

	public static final int TAG_TAIL_PASL_REQ_KEY	= 0x1C3D0601;	// Request Data Set Verimatrix PASL Server
	public static final int TAG_TAIL_PASL_RESP_KEY	= 0x1C3D0602;	// Response Data Set Verimatrix PASL Server

	public static final int TAG_TAIL_PASL_REQ_STATUS	= 0X1C3D0603; 	// Request Status of PASL Server : VMX Client -> VMX Server
	public static final int TAG_TAIL_PASL_RESP_STATUS	= 0X1C3D0604; 	// Response Status of PASL Server : VMX Server -> VMX Client

	public static final int TAG_TAIL_PUBLIC_KEY	= 0x1C3D0701;
	public static final int TAG_TAIL_AES_KEY	= 0x1C3D0702;

	// for PreTest
	public static final int TAG_TAIL_REQ_PRETEST_1 	= 0x1C3D0801 ;
	public static final int TAG_TAIL_REQ_PRETEST_2 	= 0x1C3D0802 ;
	public static final int TAG_TAIL_REQ_PRETEST_3 	= 0x1C3D0803 ;
	public static final int TAG_TAIL_REQ_PRETEST_4 	= 0x1C3D0804 ;

	public static final int TAG_TAIL_RESP_PRETEST_1 	= 0x1C3D0831 ;
	public static final int TAG_TAIL_RESP_PRETEST_2 	= 0x1C3D0832 ;
	public static final int TAG_TAIL_RESP_PRETEST_3 	= 0x1C3D0833 ;
	public static final int TAG_TAIL_RESP_PRETEST_4 	= 0x1C3D0834 ;

	// for Request Data
	public static final int TAG_TAIL_DATA_1 	= 0x1C3D0851 ;
	public static final int TAG_TAIL_DATA_2 	= 0x1C3D0852 ;
	public static final int TAG_TAIL_DATA_3 	= 0x1C3D0853 ;
	public static final int TAG_TAIL_DATA_4 	= 0x1C3D0854 ;


// Tag - Tail New Type
	public static final int TAG_TAIL_DB_CLIENT_11 	= 0x2307DC11 ;
	public static final int TAG_TAIL_DB_CLIENT_12 	= 0x2307DC12 ;
	public static final int TAG_TAIL_DB_CLIENT_13 	= 0x2307DC13 ;
	public static final int TAG_TAIL_DB_CLIENT_14 	= 0x2307DC14 ;
	public static final int TAG_TAIL_DB_CLIENT_21 	= 0x2307DC21 ;
	public static final int TAG_TAIL_DB_CLIENT_22 	= 0x2307DC22 ;
	public static final int TAG_TAIL_DB_CLIENT_23 	= 0x2307DC23 ;
	public static final int TAG_TAIL_DB_CLIENT_24 	= 0x2307DC24 ;


	public static final int TAG_TAIL_FACTORY_11 	= 0x2307FA11 ;
	public static final int TAG_TAIL_FACTORY_12 	= 0x2307FA12 ;
	public static final int TAG_TAIL_FACTORY_21 	= 0x2307FA21 ;
	public static final int TAG_TAIL_FACTORY_22 	= 0x2307FA22 ;
	public static final int TAG_TAIL_FACTORY_23 	= 0x2307FA23 ;
	public static final int TAG_TAIL_FACTORY_31 	= 0x2307FA31 ;
	public static final int TAG_TAIL_FACTORY_32 	= 0x2307FA32 ;


	// Tag - DB Client -> Dongle
	public static final int TAG_SERIAL_NUMBER	= 0x44010001;
	public static final int TAG_RELIANCE_SN		= 0x44010002;
	public static final int TAG_PCB_SN 		= 0x44010003 ;

	public static final int TAG_KEY_1 		= 0x44011001 ;
	public static final int TAG_KEY_2 		= 0x44011002 ;

	public static final int TAG_MAC_IP4_ETHERNET	= 0x44020001;
	public static final int TAG_MAC_IP4_WIFI	= 0x44020002;
	public static final int TAG_MAC_IP4_WIFI_2ND	= 0x44020003;
	public static final int TAG_MAC_IP4_BT		= 0x44020004;

	public static final int TAG_HDCP_1X		= 0x44030001;
	public static final int TAG_HDCP_22_TX		= 0x44030002;
	public static final int TAG_WIDEVINE		= 0x44030101;
	public static final int TAG_ATTESTATION		= 0x44030102;
	public static final int TAG_NETFLIX_MGKID	= 0x44030103;
	public static final int TAG_WIDEVINE_CAS	= 0x44030104;

	// Common Key
	public static final int TAG_PLAYREADY		= 0x44030201;
	public static final int TAG_PLAYREADY_PRIVATE	= 0x44030202;
	public static final int TAG_PLAYREADY_PUBLIC	= 0x44030203;
	public static final int TAG_OEM_KEY		= 0x44030204;
	public static final int TAG_ACS_KEY		= 0x44030205;

	public static final int TAG_COMMON_KEY_1	= 0x44030301;
	public static final int TAG_COMMON_KEY_2	= 0x44030302;
	public static final int TAG_COMMON_KEY_3	= 0x44030303;


	// Name ( Module ID, EMMC Model, etc... )
	public static final int TAG_NAME_WIFI_BT_MODULE	= 0x44030401 ;
	public static final int TAG_NAME_EMMC 		= 0x44030402 ;


	// Field Key
	public static final int TAG_SYNAPTICS_INIT_IMAGE 	= 0x44040001;	// match DB Client TCP Protocol pptx

	public static final int TAG_TELECHIPS_HDCP_1X	= 0x44040101;
	public static final int TAG_TELECHIPS_WIDEVINE	= 0x44040102;

	public static final int TAG_REALTEK_KEY_BAG		= 0x44040201;
	public static final int TAG_REALTEK_KEY_BAG_HASH	= 0x44040202;

	public static final int TAG_AMLOGIC_EEPROM_SN		= 0x44050101;

	// Tag - CAS - VeriMatrix
	public static final int TAG_VMX				= 0x45010000;
	public static final int TAG_VMX_INIT_NSC_COMM		= (TAG_VMX | 0x0001);	// Device -> DB Client (Result of BC_InitNscComm() call)
	public static final int TAG_VMX_CHIP_ID			= (TAG_VMX | 0x0002);	// Device -> DB Client
	public static final int TAG_VMX_INIT_NSC		= (TAG_VMX | 0x0003);	// Device -> DB Client (Result of BC_InitNsc() call)
	public static final int TAG_VMX_NSC			= (TAG_VMX | 0x0004);	// DB Client -> Device (DataSet)
	public static final int TAG_VMX_PIN			= (TAG_VMX | 0x0005);	// DB Client -> Device (DataSet)
	public static final int TAG_VMX_STB_DATA		= (TAG_VMX | 0x0006);	// DB Client -> Device (DataSet)
	public static final int TAG_VMX_VENDOR_ID		= (TAG_VMX | 0x0007);	// DB Client -> Device
	public static final int TAG_VMX_PROVIDER_ID		= (TAG_VMX | 0x0008);	// DB Client -> Device
	public static final int TAG_VMX_VENDOR_DATA		= (TAG_VMX | 0x0009);	// DB Client -> Device
	public static final int TAG_VMX_PROVIDER_DATA		= (TAG_VMX | 0x000A);	// DB Client -> Device

	public static final int TAG_VMX_PASL_SERVER_STATUS	= (TAG_VMX | 0x0010); // VMX Server -> VMX Client

	// Tag - CAS - Nagra 
	public static final int TAG_NAGRA			= 0x45020000;
	public static final int TAG_NAGRA_PK			= 0x45020001;
	public static final int TAG_NAGRA_CSC			= 0x45020002;
	public static final int TAG_NAGRA_FPK			= 0x45020003;

	public static final int TAG_NAGRA_CHIPSET_EXT		= 0x45020101;
	public static final int TAG_NAGRA_CHIPSET_CUT		= 0x45020102;
	public static final int TAG_NAGRA_NUID			= 0x45020103;
	public static final int TAG_NAGRA_NUID_CHK_NUM		= 0x45020104;
	public static final int TAG_NAGRA_STB_CASN		= 0x45020105;
	public static final int TAG_NAGRA_CSCD_CONFIG		= 0x45020106;
	public static final int TAG_NAGRA_CSCD_CHK_NUM		= 0x45020107;
	public static final int TAG_NAGRA_CERT_CHK_NUM		= 0x45020108;

	// Tag - Crypto
	public static final int TAG_PUBLIC_KEY			= 0x45030001;
	public static final int TAG_AES_128_KEY			= 0x45030002;
	public static final int TAG_AES_16_IV			= 0x45030003;

	// Reserved TAG - VMX Server - 	0x4504....
	// 0x4504....

	// Tag - Dongle -> DB Client
	public static final int TAG_TEXT_MODEL			= 0x77010001;
	public static final int TAG_VER_FACTORYSW		= 0x77010002;
	public static final int TAG_VER_MAINSW			= 0x77010003;
	public static final int TAG_VER_HW			= 0x77010004;
	public static final int TAG_CHIP_ID			= 0x77010005;
	public static final int TAG_VER_SW_1			= 0x77010006;
	public static final int TAG_VER_SW_2			= 0x77010007;
	public static final int TAG_MODEL_DATA_1		= 0x77010008;

	// New Tag - Dongle -> DB Client
	public static final int TAG2_TEXT_MODEL 	= 0x77010011 ;
	public static final int TAG2_VER_FACTORYSW 	= 0x77010012 ;
	public static final int TAG2_VER_MAINSW 	= 0x77010013 ;
	public static final int TAG2_VER_HW 		= 0x77010014 ;
	public static final int TAG2_CHIP_ID 		= 0x77010015 ;
	public static final int TAG2_VER_SW_1 		= 0x77010016 ;
	public static final int TAG2_VER_SW_2 		= 0x77010017 ;
	public static final int TAG2_MODEL_DATA_1 	= 0x77010018 ; 	// Don't use

	public static final int TAG2_DATA_1 		= 0x77010021 ;
	public static final int TAG2_DATA_2 		= 0x77010022 ;
	public static final int TAG2_DATA_3 		= 0x77010023 ;
	public static final int TAG2_DATA_4 		= 0x77010024 ;
	public static final int TAG2_DATA_5 		= 0x77010025 ;

	// WIFI
	public static final int TAG_CAL_WIFI_24			= 0x77020101;
	public static final int TAG_CAL_WIFI_24_A		= 0x77020102;
	public static final int TAG_CAL_WIFI_24_B		= 0x77020103;
	public static final int TAG_CAL_WIFI_50			= 0x77020104;
	public static final int TAG_CAL_WIFI_50_A		= 0x77020105;
	public static final int TAG_CAL_WIFI_50_B		= 0x77020106;
	public static final int TAG_CAL_WIFI			= 0x77020107;

	public static final int TAG_CAL_WIFI_24_A_1ST		= 0x77020111;
	public static final int TAG_CAL_WIFI_24_B_1ST		= 0x77020112;
	public static final int TAG_CAL_WIFI_50_A_1ST		= 0x77020113;
	public static final int TAG_CAL_WIFI_50_B_1ST		= 0x77020114;

	public static final int TAG_CAL_WIFI_24_A_2ND		= 0x77020121;
	public static final int TAG_CAL_WIFI_24_B_2ND		= 0x77020122;
	public static final int TAG_CAL_WIFI_50_A_2ND		= 0x77020123;
	public static final int TAG_CAL_WIFI_50_B_2ND		= 0x77020124;

	// Temperature
	public static final int TAG_CAL_TEMP_CPU		= 0x77020201;
	public static final int TAG_CAL_TEMP_CPU_OPEN		= 0x77020202;
	public static final int TAG_CAL_TEMP_CPU_CLOSE		= 0x77020203;

	// Voltage & Audio
	public static final int TAG_CAL_VOLTAGE			= 0x77020301;

	public static final int TAG_AUDIO_11_FREQ_L 	= 0x77020311 ;
	public static final int TAG_AUDIO_11_FREQ_R 	= 0x77020312 ;
	public static final int TAG_AUDIO_11_VPP_L 	= 0x77020313 ;
	public static final int TAG_AUDIO_11_VPP_R 	= 0x77020314 ;
	public static final int TAG_AUDIO_12_FREQ_L 	= 0x77020315 ;
	public static final int TAG_AUDIO_12_FREQ_R 	= 0x77020316 ;
	public static final int TAG_AUDIO_12_VPP_L 	= 0x77020317 ;
	public static final int TAG_AUDIO_12_VPP_R 	= 0x77020318 ;

	public static final int TAG_AUDIO_21_FREQ_L 	= 0x77020321 ;
	public static final int TAG_AUDIO_21_FREQ_R 	= 0x77020322 ;
	public static final int TAG_AUDIO_21_VPP_L 	= 0x77020323 ;
	public static final int TAG_AUDIO_21_VPP_R 	= 0x77020324 ;
	public static final int TAG_AUDIO_22_FREQ_L 	= 0x77020325 ;
	public static final int TAG_AUDIO_22_FREQ_R 	= 0x77020326 ;
	public static final int TAG_AUDIO_22_VPP_L 	= 0x77020327 ;
	public static final int TAG_AUDIO_22_VPP_R 	= 0x77020328 ;


	// MIC
	public static final int TAG_CAL_MIC_1_1KHZ_OPEN		= 0x77020401;
	public static final int TAG_CAL_MIC_2_1KHZ_OPEN		= 0x77020402;
	public static final int TAG_CAL_MIC_3_1KHZ_OPEN		= 0x77020403;
	public static final int TAG_CAL_MIC_4_1KHZ_OPEN		= 0x77020404;

	public static final int TAG_CAL_MIC_1_400HZ_OPEN	= 0x77020405;
	public static final int TAG_CAL_MIC_2_400HZ_OPEN	= 0x77020406;
	public static final int TAG_CAL_MIC_3_400HZ_OPEN	= 0x77020407;
	public static final int TAG_CAL_MIC_4_400HZ_OPEN	= 0x77020408;

	public static final int TAG_CAL_MIC_1_1KHZ_CLOSE	= 0x77020409;
	public static final int TAG_CAL_MIC_2_1KHZ_CLOSE	= 0x7702040A;
	public static final int TAG_CAL_MIC_3_1KHZ_CLOSE	= 0x7702040B;
	public static final int TAG_CAL_MIC_4_1KHZ_CLOSE	= 0x7702040C;

	public static final int TAG_CAL_MIC_1_400HZ_CLOSE	= 0x7702040D;
	public static final int TAG_CAL_MIC_2_400HZ_CLOSE	= 0x7702040E;
	public static final int TAG_CAL_MIC_3_400HZ_CLOSE	= 0x7702040F;
	public static final int TAG_CAL_MIC_4_400HZ_CLOSE	= 0x77020410;

	// Bluetooth
	public static final int TAG_CAL_BT			= 0x77020501;
	public static final int TAG_CAL_BT_1ST			= 0x77020502;
	public static final int TAG_CAL_BT_2ND			= 0x77020503;

	// System
	public static final int TAG_CAL_CHIP_ID			= 0x77020601; 	// compare with TAG_CHIP_ID
	public static final int TAG_CAL_WORKING_TIME_MIN 	= 0x77020602; 	// WoringTime_Minutes

	// FPing
	public static final int TAG_CAL_FPING_LOST 		= 0x77020701 ;
	public static final int TAG_CAL_FPING_TOTAL 		= 0x77020702 ;

	// PreTest
	public static final int TAG_CAL_PRETEST_1 		= 0x77020801 ;
	public static final int TAG_CAL_PRETEST_2 		= 0x77020802 ;
	public static final int TAG_CAL_PRETEST_3 		= 0x77020803 ;
	public static final int TAG_CAL_PRETEST_4 		= 0x77020804 ;

	public static final int TAG_CAL_PRETEST_5 		= 0x77020805 ;
	public static final int TAG_CAL_PRETEST_6 		= 0x77020806 ;
	public static final int TAG_CAL_PRETEST_7 		= 0x77020807 ;
	public static final int TAG_CAL_PRETEST_8 		= 0x77020808 ;

	// STB DATA
	public static final int TAG_STB_DATA_11 	= 0x77020911 ;
	public static final int TAG_STB_DATA_12 	= 0x77020912 ;
	public static final int TAG_STB_DATA_13 	= 0x77020913 ;
	public static final int TAG_STB_DATA_14 	= 0x77020914 ;
	public static final int TAG_STB_DATA_15 	= 0x77020915 ;
	public static final int TAG_STB_DATA_16 	= 0x77020916 ;
	public static final int TAG_STB_DATA_17 	= 0x77020917 ;
	public static final int TAG_STB_DATA_18 	= 0x77020918 ;
	public static final int TAG_STB_DATA_19 	= 0x77020919 ;
	public static final int TAG_STB_DATA_20 	= 0x7702091A ;

	public static final int TAG_STB_DATA_21 	= 0x7702091B ;
	public static final int TAG_STB_DATA_22 	= 0x7702091C ;
	public static final int TAG_STB_DATA_23 	= 0x7702091D ;
	public static final int TAG_STB_DATA_24 	= 0x7702091E ;
	public static final int TAG_STB_DATA_25 	= 0x7702091F ;
	public static final int TAG_STB_DATA_26 	= 0x77020920 ;
	public static final int TAG_STB_DATA_27 	= 0x77020921 ;
	public static final int TAG_STB_DATA_28 	= 0x77020922 ;
	public static final int TAG_STB_DATA_29 	= 0x77020923 ;
	public static final int TAG_STB_DATA_30 	= 0x77020924 ;

	// Error
	public static final int TAG_ERROR_1		= 0x77030101;
	public static final int TAG_ERROR_2		= 0x77030102;
	public static final int TAG_ERROR_3		= 0x77030103;
	public static final int TAG_ERROR_4		= 0x77030104;
	public static final int TAG_ERROR_5		= 0x77030105;

	// Log
	public static final int TAG_LOG_DEVICE_1	 = 0x77040101;
	public static final int TAG_LOG_DEVICE_2	 = 0x77040102;

	// Written Info
	public static final int TAG_WRITTEN_MAC_ETH	= 0x77050101;
	public static final int TAG_WRITTEN_MAC_WIFI	= 0x77050102;
	public static final int TAG_WRITTEN_MAC_BT	= 0x77050103;

	public static final int TAG_WRITTEN_OEM_KEY	= 0x77050104;
	public static final int TAG_WRITTEN_ACS_KEY	= 0x77050105;

	public static final int TAG_WRITTEN_SN		= 0x77050106;
	public static final int TAG_WRITTEN_FLAG	= 0x77050107;
	public static final int TAG_WRITTEN_PCB_SN 	= 0x77050108 ;

	public static final int TAG_WRITTEN_KEY_1 	= 0x77051101 ;
	public static final int TAG_WRITTEN_KEY_2 	= 0x77051102 ;


	// Data Check
	public static final int TAG_DATA_CHECK_SN	= 0x77060101;
	public static final int TAG_DATA_CHECK_ETH	= 0x77060102;
	public static final int TAG_DATA_CHECK_WIFI	= 0x77060103;
	public static final int TAG_DATA_CHECK_BT	= 0x77060104;
	public static final int TAG_DATA_CHECK_ACS_KEY	= 0x77060105;
	public static final int TAG_DATA_CHECK_NSC	= 0x77060106;


	// Factory Test Result
	public static final int TAG_FA_TEST_VALUE 	= 0x77070101 ;
	public static final int TAG_FA_TEST_STATUS 	= 0x77070102 ;

	public static final int TAG_TAIL_FA_TEST_1 	= 0x77070201 ;
	public static final int TAG_TAIL_FA_TEST_2 	= 0x77070202 ;
	public static final int TAG_TAIL_FA_TEST_3 	= 0x77070203 ;
	public static final int TAG_TAIL_FA_TEST_4 	= 0x77070204 ;
	public static final int TAG_TAIL_FA_TEST_5 	= 0x77070205 ;
	public static final int TAG_TAIL_FA_TEST_6 	= 0x77070206 ;
	public static final int TAG_TAIL_FA_TEST_7 	= 0x77070207 ;
	public static final int TAG_TAIL_FA_TEST_8 	= 0x77070208 ;
	public static final int TAG_TAIL_FA_TEST_9 	= 0x77070209 ;
	public static final int TAG_TAIL_FA_TEST_10 	= 0x7707020a ;
	public static final int TAG_TAIL_FA_TEST_11 	= 0x7707020b ;
	public static final int TAG_TAIL_FA_TEST_12 	= 0x7707020c ;
	public static final int TAG_TAIL_FA_TEST_13 	= 0x7707020d ;
	public static final int TAG_TAIL_FA_TEST_14 	= 0x7707020e ;
	public static final int TAG_TAIL_FA_TEST_15 	= 0x7707020f ;
	public static final int TAG_TAIL_FA_TEST_16 	= 0x77070210 ;

	public static final int SIZE_MAC_IP4_ETHERNET	= 17;
	public static final int SIZE_HDCP_AMLG_1X	= 308;

}
