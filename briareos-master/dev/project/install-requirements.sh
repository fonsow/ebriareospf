apt update
apt install -y $(cat packages.txt)
pip install -r requirements.txt