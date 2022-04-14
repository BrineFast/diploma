from typing import Dict, List, NoReturn

import numpy as np
import tensorflow as tf
import cv2

model_path: str = '"utils/mobilenet_ssd.tflite"'

class Model:
    def __init__(self, interpreter: tf.lite.Interpreter):
        self.detected = dict()
        self.tf = interpreter

    def detect(self,
               output_details: Dict[str, int],
               iou_thresh: float = 0.5,
               score_thresh: float = 0.6) -> NoReturn:
        self.detected: Dict[str, int] = {
            'highlight': self.tf.get_tensor(output_details[0]['index'])[0],
            'class': self.tf.get_tensor(output_details[1]['index'])[0],
            'score': self.tf.get_tensor(output_details[2]['index'])[0]
        }

        self.detected['class'] = self.detected['class'].astype(np.int64)
        self.apply(iou_thresh, score_thresh)

    def apply(self,
              iou_thresh: float = 0.5,
              score_thresh: float = 0.6) -> NoReturn:
        num: int = int(self.detected['num_detections'])
        boxes: np.array = np.zeros([1, num, 90, 4])
        scores: np.array = np.zeros([1, num, 90])

        for i in range(num):
            boxes[0, i, self.detected['class'][i], :] = self.detected['highlight'][i]
            scores[0, i, self.detected['class'][i]] = self.detected['score'][i]

        supp: object = tf.image.combined_non_max_suppression(boxes=boxes,
                                                             scores=scores,
                                                             max_output_size_per_class=num,
                                                             max_total_size=num,
                                                             iou_threshold=iou_thresh,
                                                             score_threshold=score_thresh,
                                                             pad_per_class=False,
                                                             clip_boxes=False)
        valid: np.array = supp.valid_detections[0].numpy()
        self.detected = {
            'highlight': supp.nmsed_boxes[0].numpy()[:valid],
            'class': supp.nmsed_classes[0].numpy().astype(np.int64)[:valid],
            'score': supp.nmsed_scores[0].numpy()[:valid],
        }

    def highlight_detection(self,
                            img: np.array,
                            input_details: List[Dict],
                            output_details: Dict[str, int],
                            score_thresh=0.6,
                            iou_thresh=0.5):
        img_rgb: np.array = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        img_rgb = cv2.resize(img_rgb, (300, 300), cv2.INTER_AREA)
        img_rgb = img_rgb.reshape([1, 300, 300, 3])

        self.tf.set_tensor(input_details[0]['index'], img_rgb)
        self.tf.invoke()

        self.detect(output_details, iou_thresh, score_thresh)


if __name__ == "__main__":
    interpreter: tf.lite.Interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

    input_details: np.array = interpreter.get_input_details()
    output_details: np.array = interpreter.get_output_details()

    input_shape: int = input_details[0]['shape']
    cap: np.array = cv2.VideoCapture(0)
    model = Model(interpreter)

    while (True):
        ret, img = cap.read()
        if ret:
            model.highlight_detection(img, input_details, output_details)
            cv2.imshow("image", img)
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break
        else:
            break

    cap.release()
    cv2.destroyAllWindows()
