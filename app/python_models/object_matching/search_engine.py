from typing import List

import numpy as np
import datetime
import os

from flask import Flask, request, Response, jsonify

from object_matching.model_search_engine import Model
from object_matching.utils.image_encoder import encode_image, decode_images

app: Flask = Flask(__name__)

image_dir: str = "storage/"

search_engine: Model = Model(image_dir)


@app.route('/inference', methods=['POST'])
def get_tasks():
    if not request.json or not 'title' in request.json:
        return Response("Waiting for image", status=400)

    image_bits: np.array = np.array((request.json["body"]["image"]))

    image_name: str = os.path.join(image_dir, f"{datetime.datetime.now().timestamp()}.png")
    encode_image(image_bits, image_name)

    results: List[str] = search_engine.inference(image_name)
    images: List[np.array] = decode_images(results)
    return jsonify(images)


if __name__ == "__main__":
    search_engine.run()
    app.run()
