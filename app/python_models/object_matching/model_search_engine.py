import os
from typing import NoReturn

import numpy as np
import faiss

from collections import defaultdict

import torch
from torch.utils.data import DataLoader
from torchvision.models import mnasnet1_0, MNASNet
from tqdm import tqdm

from object_matching.utils.dataset import ImageDataSet


class Model:
    def __init__(self, path_to_dir: str):
        self._model: MNASNet = mnasnet1_0(pretrained=True)
        self._index: faiss.IndexFlatL2 = self._load_index()
        self._path_to_dir: str = path_to_dir
        self.similarity_matrix = defaultdict(lambda: set())
        self.indexes = list()

        self._prepare_model()
        self.images = self._retrieve_images()

    def run(self) -> NoReturn:
        batch_loader: DataLoader = DataLoader(ImageDataSet(self.images),
                                              batch_size=25,
                                              num_workers=0)
        self._train(batch_loader)
        self._test()

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

    def _train(self, batch_loader: DataLoader) -> NoReturn:
        embedings: list = list()
        inf_list: list = list()

        with torch.no_grad():
            for batch in tqdm(batch_loader):
                image = batch.to('cuda')
                embedings.append(self._model(image))
                del image
                torch.cuda.empty_cache()

        for v in embedings:
            for el in v.tolist():
                el = np.float32(np.array([el]))
                self._index.add(el)
                self.indexes.append(el)
                inf_list.append(el)

        return inf_list

    def _test(self) -> NoReturn:
        excluded: dict = dict()

        for img_hash in tqdm(self.indexes):
            s: list = self._match(img_hash)
            bucket_images: list = list(map(lambda l: l[0], filter(lambda x: x[0] not in excluded and x[1] < 2500, s)))
            lst: list = map(lambda l: self.images[l], bucket_images)
            self.similarity_matrix[excluded.get(s[0][0], s[0][0])].update(lst)
            excluded.update([(k, s[0][0]) for k in bucket_images])

    def inference(self, image: np.array) -> list:
        batch_loader: DataLoader = DataLoader(ImageDataSet(image),
                                              batch_size=1,
                                              num_workers=0)
        hash = self._train(batch_loader)[0]

        s: list = self._match(hash)
        if image[0] not in self.images:
            self.images.append(image[0])

        bucket_images: list = list(map(lambda l: l[0], filter(lambda x: x[1] < 2500, s)))
        print(s)
        similarity: list = list(map(lambda l: self.images[l], bucket_images))

        return similarity

    def _match(self, img_hash: np.ndarray) -> list:
        D, I = self._index.search(img_hash, len(os.listdir(self._path_to_dir)))
        return list(zip(I[0], D[0]))

    def _retrieve_images(self) -> list:
        return list(map(lambda x: os.path.join(self._path_to_dir, x),
                        os.listdir(self._path_to_dir)))

    @staticmethod
    def _load_index() -> faiss.IndexFlatL2:
        try:
            return faiss.read_index_binary('faiss_index_1000')
        except RuntimeError:
            return faiss.IndexFlatL2(1000)