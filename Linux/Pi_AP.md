# 라즈베리 파이를 Wi-Fi AP로 설정하고, dnscrypt-proxy (DoQ) 를 사용하여 클라이언트들에게 보안 DNS를 제공.

✅ 네트워크 구성도

[인터넷(외부망)]
       │
[공유기 1 (SKB)]
       │
[공유기 2 (TP-Link)]
       │
 ┌─────┴──────┐
 │      [Raspberry Pi]
 │        └── dnscrypt-proxy (QUIC DNS Proxy)
[PC / 노트북 / 테스트 디바이스]

1️⃣ 기본 패키지 설치

sudo apt update && sudo apt install -y hostapd dnsmasq iptables-persistent

2️⃣ Wi-Fi AP 설정 (wlan0 사용)

/etc/dhcpcd.conf

interface wlan0
    static ip_address=192.168.50.1/24
    nohook wpa_supplicant

/etc/dnsmasq.conf

interface=wlan0
dhcp-range=192.168.50.10,192.168.50.100,12h

/etc/hostapd/hostapd.conf

interface=wlan0
driver=nl80211
ssid=QUIC_AP
hw_mode=g
channel=7
wmm_enabled=0
macaddr_acl=0
auth_algs=1
ignore_broadcast_ssid=0
wpa=2
wpa_passphrase=yourpassword
wpa_key_mgmt=WPA-PSK
rsn_pairwise=CCMP

/etc/default/hostapd

DAEMON_CONF="/etc/hostapd/hostapd.conf"

서비스 활성화

sudo systemctl unmask hostapd
sudo systemctl enable hostapd
sudo systemctl restart hostapd

3️⃣ IP 포워딩 및 NAT 설정

sudo sysctl -w net.ipv4.ip_forward=1
sudo iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
sudo sh -c "iptables-save > /etc/iptables/rules.v4"

## DNS 설정 

4️⃣ systemd-resolved 비활성화 및 포트 53 확보

sudo lsof -i :53

sudo systemctl stop systemd-resolved

sudo systemctl disable systemd-resolved

sudo rm /etc/resolv.conf

echo "nameserver 127.0.0.1" | sudo tee /etc/resolv.conf

sudo lsof -i :53

5️⃣ dnscrypt-proxy 설치 및 설정 (QUIC 사용) 바이너리 설치 (ARM용)

일단 dnscrypt-proxy 를 다운 받아서 설정을 해야 한다. 

운영체제 아키텍처 먼저 확인

uname -m 

armv71 > arm32

aarch64 > arm64

(라즈베리는 arm64기반이라 잘 골라서 해야함)

여기서 주로 작업을 하라함

cd /opt  

sudo wget https://github.com/DNSCrypt/dnscrypt-proxy/releases/download/2.1.8/dnscrypt-proxy-linux_arm64-2.1.8.tar.gz

sudo tar -xvzf dnscrypt-proxy-linux_arm64-2.1.8.tar.gz

이러면 linux_arm64-2.1.8 폴더가 생성됨. 

sudo mkdir -p dnscrypt-proxy-linux_arm64

dnscrypt-proxy-linux_arm64 폴더 안에 dnscrypt-proxy.toml 와 dnscrypt-proxy 파일을 cp 해둔다.

설정 파일 수정

sudo vi /opt/dnscrypt-proxy-linux_arm64/dnscrypt-proxy.toml

server_names = ['cloudflare']
listen_addresses = ['127.0.0.1:53', '[::1]:53']
ipv6_servers = false
dnscrypt_servers = true
doh_servers = true
odoh_servers = false
require_dnssec = true
force_tcp = false
timeout = 2500
keepalive = 30
use_syslog = true
netprobe_timeout = 60
netprobe_address = '9.9.9.9:53'
block_ipv6 = false

systemd 서비스 파일 생성

sudo vi /etc/systemd/system/dnscrypt-proxy.service

[Unit]
Description=dnscrypt-proxy
After=network.target

[Service]
ExecStart=/opt/dnscrypt-proxy-linux_arm64/dnscrypt-proxy -config /opt/dnscrypt-proxy-linux_arm64/dnscrypt-proxy.toml
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
-------------------

sudo systemctl daemon-reexec

sudo systemctl daemon-reload

sudo systemctl enable dnscrypt-proxy

sudo systemctl start dnscrypt-proxy

sudo systemctl status dnscrypt-proxy

6️⃣ DNS 확인
dig @127.0.0.1 www.cloudflare.com # 응답이 나오면 정상 작동

sudo journalctl -u dnscrypt-proxy | grep -i quic








# QUIC 사용
doh_upgrade = true 

<hostapd.conf>

interface=wlan0
driver=nl80211
ssid=QUIC_AP
hw_mode=g
channel=6
wmm_enabled=1
auth_algs=1
wpa=2
wpa_passphrase=your_secure_password
wpa_key_mgmt=WPA-PSK
rsn_pairwise=CCMP


🔧 interface 모드 확인 (AP 모드 지원 여부)
iw list | grep -A 10 "Supported interface modes"

🔧 인터페이스가 다른 데서 점유 중인지 확인
sudo systemctl stop NetworkManager
sudo systemctl stop wpa_supplicant

sudo ip link set wlan0 down
sudo ip link set wlan0 up

🔧 hostapd 다시 실행
sudo systemctl unmask hostapd
sudo systemctl enable hostapd
sudo systemctl restart hostapd
sudo systemctl status hostapd

🔧 현재 Wi-Fi 네트워크 연결 끊기
sudo nmcli dev disconnect iface wlan0

🔧 wlan0을 AP 모드로 설정
sudo ip link set wlan0 down
sudo iw dev wlan0 set type ap
sudo ip link set wlan0 up

## DNS 설정
53번 포트가 아무것도 안잡혀 있는 상태에서 dns가 AP용으로 잡혀야한다.

sudo lsof -i :53
sudo systemctl stop systemd-resolved
sudo systemctl disable systemd-resolved

sudo rm -f /etc/resolv.conf
echo "nameserver 127.0.0.1" | sudo tee /etc/resolv.conf

sudo vi /etc/nsswitch.conf
```hosts: files dns 으로 변경```

sudo nano /etc/systemd/resolved.conf
```DNSStubListener=no```

sudo systemctl mask systemd-resolved

reboot

---------

sudo systemctl disable systemd-resolved
sudo systemctl stop systemd-resolved

🔍 아무것도 안 나오면 성공! 이 상태에서:
sudo systemctl restart dnscrypt-proxy
sudo systemctl status dnscrypt-proxy

 🌐 dnscrypt-proxy는 정상 작동 중인가?
sudo systemctl status dnscrypt-proxy

### log 및 상태 체크 
Wi-Fi 인터페이스 상태 확인
iwconfig

AP 기능 문제시 로그 추출
sudo journalctl -u hostapd

DNS 기능 문제시 로그 추출
sudo journalctl -u dnscrypt-proxy --no-pager -xe

