from typing import NoReturn, List

import numpy as np
import cv2


def encode_image(bits: np.array, image_name: str) -> NoReturn:
    to_image: np.array = np.packbits(bits)
    to_image.tofile(image_name)


def decode_images(images: List[str]) -> List[np.array]:
    image_bit: List[np.array] = list()

    for image in images[1:]:
        img: np.array = cv2.imread(image)
        success, encoded_image = cv2.imencode('.png', img)
        image_bit.append(encoded_image.tobytes())

    return image_bit
