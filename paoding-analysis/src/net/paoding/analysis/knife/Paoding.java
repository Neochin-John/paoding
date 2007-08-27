/**
 * Copyright 2007 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.paoding.analysis.knife;

import net.paoding.analysis.dictionary.support.detection.Detector;
import net.paoding.analysis.dictionary.support.detection.Difference;
import net.paoding.analysis.dictionary.support.detection.DifferenceListener;
import net.paoding.analysis.dictionary.support.detection.ExtensionFileFilter;
import net.paoding.analysis.dictionary.support.detection.Snapshot;

/**
 * Paoding是一个背着“刀箱”(内藏各种“刀”)毕生精力“解牛”的人，即“庖丁”。
 * <p>
 * 正因为他拥有各种不同的“刀”，而且能够识别什么“肉(字符)”应该用什么“刀”分割，所以他能游刃有余地把整头牛切割，成为合适的“肉片(词语)”。 <br>
 * 这里的“刀”由Knife扮演，各种“刀”由“刀箱”KnifeBox管理(Paoding对象本身就是一个KnifeBox)，并由KnifeBox决策什么时候出什么“刀”。
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @see Knife
 * @see KnifeBox
 * 
 * @since 1.0
 */
public class Paoding extends SmartKnifeBox implements Knife {


	private Dictionaries dictionaries;
	
	private String dicHomeAbsolutePath;
	
	private int interval;
	
	private Detector detector;

	private Snapshot lastSnapshot;

	public Paoding() {
	}

	// -------------------------------------------------

	public String getDicHomeAbsolutePath() {
		return dicHomeAbsolutePath;
	}

	public void setDicHomeAbsolutePath(String dicHomeAbsolutePath) {
		this.dicHomeAbsolutePath = dicHomeAbsolutePath;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public Dictionaries getDictionaries() {
		return dictionaries;
	}

	public void setDictionaries(Dictionaries dictionaries) {
		this.dictionaries = dictionaries;
	}

	public Snapshot getLastSnapshot() {
		return lastSnapshot;
	}

	public void setLastSnapshot(Snapshot lastSnapshot) {
		this.lastSnapshot = lastSnapshot;
	}

	/**
	 * 启动字典动态转载/卸载检测器 侦测时间间隔(秒)。<br>
	 * 默认为60秒。如果设置为０或负数则表示不需要进行检测
	 */
	public synchronized void startDetecting() {
		if (detector != null || interval < 0) {
			return;
		}
		Detector detector = new Detector();
		detector.setHome(dicHomeAbsolutePath);
		detector.setFilter(new ExtensionFileFilter(".dic"));
		detector.setLastSnapshot(detector.flash());
		setLastSnapshot(detector.getLastSnapshot());
		final DifferenceListener l = new FileDictionariesDifferenceListener(
				dictionaries, this);
		detector.setListener(new DifferenceListener() {
			public boolean on(Difference diff) {
				boolean b = l.on(diff);
				if (b) {
					lastSnapshot = diff.getYounger();
				}
				return b;
			}
		});
		detector.setInterval(interval);
		detector.start(true);
		this.detector = detector;
	}

	public synchronized void stopDetecting() {
		if (detector == null) {
			return;
		}
		detector.setStop();
		detector = null;
	}

	public void forceDetecting() {
		final Detector detector = new Detector();
		detector.setHome(this.dicHomeAbsolutePath);
		detector.setFilter(new ExtensionFileFilter(".dic"));
		final DifferenceListener l = new FileDictionariesDifferenceListener(
				dictionaries, this);
		detector.setListener(new DifferenceListener() {
			public boolean on(Difference diff) {
				detector.setStop();
				boolean b = l.on(diff);
				if (b) {
					lastSnapshot = diff.getYounger();
				}
				return b;
			}
		});
		detector.setInterval(1);
		detector.setLastSnapshot(lastSnapshot);
		detector.start(true);
	}

	/**
	 * 立即执行一次词典检查是否变更了，如果变更了则要通知Paoding中的Knives. <br>
	 * 直到检查到是否有了更新，以及做了相应的更新后才返回本方法。
	 */
	public void forceDetectingAndWaiting() {
		Detector detector = new Detector();
		detector.setHome(this.dicHomeAbsolutePath);
		detector.setFilter(new ExtensionFileFilter(".dic"));
		final DifferenceListener l = new FileDictionariesDifferenceListener(
				dictionaries, this);
		detector.setListener(new DifferenceListener() {
			public boolean on(Difference diff) {
				boolean b = l.on(diff);
				if (b) {
					lastSnapshot = diff.getYounger();
				}
				return b;
			}
		});
		detector.setLastSnapshot(lastSnapshot);
		detector.forceDetecting();
	}

}