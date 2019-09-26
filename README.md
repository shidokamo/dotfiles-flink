# Dotfiles : Flink
Flink のクラスタを運用するための設定ファイルです。

# Install Flink to local VM as master
```
make install
```

# Create worker VMs and install spark
```
make worksers
```

# クラスタの実行
```
make start-cluster
```

# クラスタのテスト
```
make test
make test2
```
