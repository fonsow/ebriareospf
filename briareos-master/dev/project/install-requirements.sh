apt update
apt install -y $(cat packages.txt)
pip3 install -r requirements.txt
