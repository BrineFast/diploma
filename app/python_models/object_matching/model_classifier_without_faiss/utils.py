import cv2
import numpy as np
import torch

from typing import NoReturn
from torch.utils.data import Dataset


class ImageDataSet(Dataset):
    def __init__(self, filenames: list) -> NoReturn:
        self._filenames: list = filenames

    @staticmethod
    def add_pad(img: np.array, shape: np.array) -> np.array:
        padded_img: np.array = img[0][0] * np.ones(shape + img.shape[2:3], dtype=np.uint8)
        x_offset: int = int((padded_img.shape[0] - img.shape[0]) / 2)
        y_offset: int = int((padded_img.shape[1] - img.shape[1]) / 2)
        padded_img[x_offset:x_offset + img.shape[0], y_offset:y_offset + img.shape[1]] = img
        return padded_img

    @staticmethod
    def resize(img: np.array, shape: np.array) -> np.array:
        scale: int = min(shape[0] * 1.0 / img.shape[0], shape[1] * 1.0 / img.shape[1])
        return cv2.resize(img, dsize=None, fx=scale, fy=scale, interpolation=cv2.INTER_LINEAR)

class TrainDataSet(ImageDataSet):
    def __init__(self, filenames: list, labels: list = None) -> NoReturn:
        super().__init__(filenames)
        self._labels: list = labels

    def __len__(self) -> int:
        return len(self._filenames)

    def __getitem__(self, idx: int) -> tuple:
        img: np.array = self.resize(cv2.imread(self._filenames[idx]), (224, 224))
        img: np.array = self.add_pad(img, (224, 224))
        img: np.array = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        img: np.array = torch.tensor(img, dtype=torch.float).permute(2, 0, 1) / 255.
        return img, self._labels[idx]

class TestDataSet(TrainDataSet):
    def __init__(self, filenames: list) -> NoReturn:
        super().__init__(filenames)

    def __len__(self) -> int:
        return len(self._filenames)

    def __getitem__(self, idx: int) -> np.array:
        img: np.array = self.resize(cv2.imread(self._filenames[idx]), (224, 224))
        img: np.array = self.add_pad(img, (224, 224))
        img: np.array = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        img: np.array = torch.tensor(img, dtype=torch.float).permute(2, 0, 1) / 255.
        return img