import os
import numpy as np
import torch
import cv2
import matplotlib.pyplot as plt

from random import random
from typing import NoReturn

from sklearn.model_selection import train_test_split
from torch.nn import CrossEntropyLoss
from torch.optim import Adam
from torch.optim.lr_scheduler import CosineAnnealingLR
from torch.utils.data import DataLoader
from torchvision.models import MNASNet, mnasnet1_0
from tqdm import tqdm

from object_matching.model_classifier_without_faiss.utils import TrainDataSet, TestDataSet


class Model():
    def __init__(self, path_to_train: str) -> NoReturn:
        self._mapping: dict = dict()
        self.train_files, self.train_labels = self._get_partitioned_file_names(path_to_train)
        self._classes_count: int = len(os.listdir(path_to_train))
        self._model: MNASNet = mnasnet1_0(pretrained=True)
        self._prepare_model()

    def train(self, epoch_count: int) -> NoReturn:
        train_filenames, test_filenames, train_labels, test_labels = \
            train_test_split(self.train_files,
                             self.train_labels,
                             test_size=0.2,
                             random_state=42)

        train: DataLoader = DataLoader(TrainDataSet(train_filenames, train_labels),
                                       shuffle=True,
                                       batch_size=25,
                                       num_workers=0)
        test: DataLoader = DataLoader(TestDataSet(test_filenames),
                                      shuffle=True,
                                      batch_size=25,
                                      num_workers=0)

        optimizer: Adam = Adam(self._model.parameters(), lr=0.0005)

        scheduler: CosineAnnealingLR = CosineAnnealingLR(optimizer=optimizer,
                                                         T_max=1000,
                                                         eta_min=0,
                                                         last_epoch=-1,
                                                         verbose=False)
        for x in tqdm(range(epoch_count)):
            for batch in train:
                self.evaluate(batch,
                              CrossEntropyLoss(),
                              optimizer)

            self._model.eval()
            test_accuracy: list = list()
            test_real: list = list()
            with torch.no_grad():
                for batch_x, batch_y in tqdm(test):
                    outputs: np.array = (self._model(batch_x.to('cuda'))
                                         .detach()
                                         .gpu()
                                         .numpy())
                    test_accuracy.append(outputs)
                    test_real.append(batch_y
                                     .detach()
                                     .gpu()
                                     .numpy())
            self._model.train()
            scheduler.step()

    def test(self, test_img_count: float, test_img: str) -> NoReturn:
        test_file_names: list = list(map(lambda x: os.path.join(test_img, x),
                                         os.listdir(test_img)))
        test_filenames = random.sample(test_file_names, test_img_count)
        test_ds: DataLoader = DataLoader(TestDataSet(test_filenames), batch_size=25, num_workers=0)
        path_num: int = 0
        for batch in test_ds:
            label_pred = self._model(batch.to('cuda'))
            for i in label_pred:
                img_class: str = self._mapping[torch.argmax(i).item()]
                plt.figure(path_num)
                plt.title(img_class)
                plt.imshow(cv2.cvtColor(cv2.imread(test_ds.dataset._filenames[path_num]), cv2.COLOR_BGR2RGB))
                plt.savefig(f"classified_{img_class}_{path_num}")
                path_num += 1
        plt.show()

    def _get_partitioned_file_names(self, path_to_ds: str) -> tuple:
        result: list = list()
        classes: list = list()
        for cl, train_dir in enumerate(os.listdir(path_to_ds)):
            self._mapping[cl] = train_dir
            for file in os.listdir(os.path.join(path_to_ds, train_dir)):
                result.append(os.path.join(path_to_ds, train_dir, file))
                classes.append(cl)
        return result, classes

    def _prepare_model(self) -> NoReturn:
        for param in self._model.parameters():
            param.requires_grad = False

        self._model.to('cuda')
        ct: int = 0
        for child in self._model.children():
            ct += 1
            if ct < 47:
                for param in child.parameters():
                    param.requires_grad = True

    def evaluate(self, batch, criterion: torch.nn.CrossEntropyLoss, optimizer: torch.optim.Adam) -> NoReturn:
        optimizer.zero_grad()
        image, label = batch
        label_pred = self._model(image.to('cuda'))
        loss = criterion(label_pred, label.to('cuda'))
        loss.backward()
        optimizer.step()

if __name__ == "__main__":
    model: Model = Model("data")
    model.train(10)
    model.test(10, "data/test")