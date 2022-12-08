from flask import Flask, session, redirect, request, url_for, render_template
from werkzeug.utils import secure_filename
import os, json, random

app = Flask(__name__, static_url_path="")

DEBUG = True
SERVER_PORT = 4000
HOST = "0.0.0.0"
UPLOAD_FOLDER = "uploads/"

app.config["UPLOAD_FOLDER"] = UPLOAD_FOLDER

def get_random_data(n=32):
    return os.urandom(n).encode("hex")

NODE_ROOT_COLOR = "#00BC8C"
NODE_DETECTOR_COLOR = "#F39C12"
NODE_DEFAULT_COLOR = "#DDDDDD"
NODE_PARSER_COLOR = "#3498DB"
NODE_HANDLER_COLOR = "#E74C3C"

def parse_pipeline_file(path, output_dir):
    f = open(path)
    s = f.read()
    f.close()
    try:
        obj = json.loads(s)
    except:
        return None

    n = 0
    e = 0

    output = {"nodes": [], "edges": []}
    id_names = {}
    modules = obj["modules"]
    added = []
    for module in modules:
        l = []
        l.append(module["name"])

        try:
            next_modules = module["next"]
            if type(next_modules) != list:
                next_modules = [next_modules]
            for next_module in next_modules:
                l.append(next_module)
        except KeyError:
            pass

        for name in l:
            if name not in added:
                node_id = "n%s" % n
                color = NODE_DEFAULT_COLOR
                if n == 0:
                    color = NODE_ROOT_COLOR
                else:
                    if "detector" in name.lower():
                        color = NODE_DETECTOR_COLOR
                    elif "parser" in name.lower():
                        color = NODE_PARSER_COLOR
                    elif "handler" in name.lower():
                        color = NODE_HANDLER_COLOR
                n += 1

                output["nodes"].append({"id": node_id, "label": name, "size": 14, "color": color})
                id_names[name] = node_id
                added.append(name)

    for module in modules:
        name = module["name"]
        try:
            next_modules = module["next"]
            if type(next_modules) != list:
                next_modules = [next_modules]

            source_id = id_names[name]

            for next_module in next_modules:
                target_id = id_names[next_module]
                edge_id = "e%s" % e
                e += 1
                output["edges"].append({"id": edge_id, "source": source_id, "target": target_id, "color": NODE_DEFAULT_COLOR})
        except KeyError:
            pass

    random_filename = get_random_data()
    output_path = os.path.join(output_dir, random_filename)
    f = open(output_path, "wb")
    f.write(json.dumps(output))
    f.close()
    return "/json/" + random_filename


@app.route("/")
def index():
    return render_template("index.html")

@app.route("/upload_pipeline", methods=["POST"])
def upload_pipeline():
    if "file" not in request.files:
        return "error"

    file = request.files["file"]
    if file.filename == "":
        return "error"

    filename = get_random_data()
    path = os.path.join(app.config["UPLOAD_FOLDER"], filename)
    file.save(path)

    sigma_path = parse_pipeline_file(path, app.static_folder + "/json/")
    if sigma_path == None:
        sigma_path = "error"

    os.remove(path)
    return sigma_path

@app.route("/pipeline")
def pipeline():
    return render_template("pipeline.html")

if __name__ == "__main__":
    app.run(HOST, SERVER_PORT, DEBUG)
