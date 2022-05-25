import os
import time
import faiss
import numpy as np

from typing import List

from flask import Flask, request, Response, jsonify

from object_matching.model_search_engine import Model
from object_matching.utils.image_encoder import encode_image, decode_images

app: Flask = Flask(__name__)

image_dir: str = "storage/"

search_engine: Model = Model(image_dir)


@app.route('/inference', methods=['POST'])
def inference():
    if not request.json or not 'title' in request.json:
        return Response("Waiting for image", status=400)

    image_bits: np.array = np.array((request.json["body"]["image"]))

    timestamp: str = str(time.time()).replace(".", "")
    image_name: str = os.path.join(image_dir, f"{timestamp}.png")
    encode_image(image_bits, image_name)

    results: List[List[str, str]] = search_engine.inference(image_name)
    images: List[np.array] = decode_images(results[0])
    return jsonify(image=images,
                   url=results[1])


if __name__ == "__main__":
    search_engine.run()
    app.run()
    faiss.write_index(search_engine._index, "last_indexes.index")
